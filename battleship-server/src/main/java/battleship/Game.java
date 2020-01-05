package battleship;

import battleship.heartbeat.HeartbeatClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Battleship game
 * <p>
 * Works with 2 players on a 10x10 Grid with 7 ships (2x1, 2x2, 1x3, 1x4, 1x5).
 * Ships are in the format "XXYYHLL"
 * and has the following meaning
 * xx column
 * yy row
 * [HV] orientation
 * LL length
 * <p>
 * After SEND_GRID, client should respond with all 7 ships joined by '_'
 * <p>
 * After GAME_START, client only needs to send "SHOOT_XXYY", 1 each turn.
 */
public class Game {
    public static final int NUM_SHIPS = 7;
    public static final int GRID_SIZE = 10;
    public static final List<Integer> AVAILABLE_SHIPS = Arrays.asList(1, 1, 2, 2, 3, 4, 5);

    private CountDownLatch playersReady = new CountDownLatch(2);
    public Player currentPlayer;

    public Game() {
        this.currentPlayer = null;
    }

    class Player implements Runnable {
        private HeartbeatClient playerSocket;
        private List<Ship> ships;
        private Player opponent;
        private List<String> shotHistory;

        public Player(HeartbeatClient playerSocket) {
            this.playerSocket = playerSocket;
            this.shotHistory = new ArrayList<>();
        }

        @Override
        public void run() {
            int x, y;
            String nextLine, shot;
            ShotResult shotResult;

            try {
                // grid disposition
                ships = setupGrid(playerSocket);
                // If there was an error like other player disconnected
                if (ships == null) {
                    // don't disconnect this player now because Scanner may have not buffered message (Windows 10 problem)
                    return;
                }

                playersReady.countDown();
                playersReady.await();

                // game start
                playerSocket.println("GAME_START");
                // initial TURN_START
                if (currentPlayer == this)
                    playerSocket.println("TURN_START");

                while (true) {
                    // Wait until a message is available. Meanwhile checks if opponent disconnected
                    while (!playerSocket.available()) {
                        if (opponent.playerSocket.isClosed()) {
                            playerSocket.println("WIN_OPPONENT_DC");
                            return;
                        } else {
                            Thread.sleep(10);
                        }
                    }

                    nextLine = playerSocket.getOneMessage();


                    // Check if it's not player's turn or a ping message has been received
                    if (currentPlayer != this || nextLine.startsWith("PING")) {
                        continue;
                    }

                    // Check command
                    if (nextLine.startsWith("SHOOT_") && nextLine.length() == 10) {
                        shot = nextLine.substring(6);

                        // check if coordinates are valid
                        try {
                            x = Integer.parseInt(shot.substring(0, 2));
                            y = Integer.parseInt(shot.substring(2, 4));
                            if (x < 1 || x > GRID_SIZE || y < 1 || y > GRID_SIZE) {
                                throw new NumberFormatException("Invalid coordinates");
                            }
                        } catch (NumberFormatException e) {
                            playerSocket.println("INVALID");
                            continue;
                        }

                        // check if shot was already thrown in this game
                        if (this.shotHistory.contains(shot)) {
                            playerSocket.println("DUPLICATE");
                            continue;
                        }

                        this.shotHistory.add(shot);

                        shotResult = this.opponent.shoot(shot);
                        switch (shotResult.getStatus()) {
                            case HIT:
                                playerSocket.println("HIT");
                                opponent.playerSocket.println("HIT_" + shot);
                                break;
                            case OCEAN:
                                playerSocket.println("OCEAN");
                                opponent.playerSocket.println("OCEAN_" + shot);
                                break;
                            case SANK:
                                playerSocket.println("SANK_" + shotResult.getSankShip().toString());
                                opponent.playerSocket.println("SANK_" + shotResult.getSankShip().toString());
                                break;
                        }

                        // check if player Won the game
                        if (opponent.hasLost()) {
                            playerSocket.println("WIN");
                            opponent.playerSocket.println("LOST_" + this.ships.stream()
                                    .map(Ship::toString)
                                    .collect(Collectors.joining("_"))
                            );
                            break;
                        }

                        // End turn and pass the game to the next player
                        currentPlayer = opponent;
                        playerSocket.println("TURN_END");
                        opponent.playerSocket.println("TURN_START");
                    } else {
                        playerSocket.println("INVALID");
                        continue;
                    }
                }
            } catch (IllegalStateException | NoSuchElementException e) {
                // Input Scanner closed or SIGINT
                opponent.playerSocket.println("WIN_OPPONENT_DC");
            } catch (InterruptedException e) {
                // Catches error while awaiting game ready state
                e.printStackTrace();
            } finally {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Check if this players has no ship left. Effectively having lost.
         *
         * @return True if player has no ship left.
         */
        private boolean hasLost() {
            return ships.size() == 0;
        }

        /**
         * Shoot in the player's grid
         *
         * @param coordinates "XXYY" coordinates of the shot.
         *                    They aren't checked for validity e.g. x={1..10}, y={1..10}. Therefore any checks must be done before.
         * @return Returns the result of the shoot
         */
        private ShotResult shoot(String coordinates) throws IllegalArgumentException {
            if (coordinates == null || coordinates.length() != 4)
                throw new IllegalArgumentException("Invalid coordinates");

            for (Ship s : ships) {
                switch (s.hit(coordinates)) {
                    case OCEAN:
                        continue;
                    case HIT:
                        return new ShotResult(Shot.HIT);
                    case SANK:
                        ships.remove(s);
                        return new ShotResult(Shot.SANK, s);
                    case DUPLICATE:
                        return new ShotResult(Shot.DUPLICATE);
                }
            }

            // if no ship was hit, then it was a miss
            return new ShotResult(Shot.OCEAN);
        }

        /**
         * Get grid from "in" and checks that is formatted correctly.
         * - Checks number of ships sent
         * - Checks format of each one
         * - Checks there's the right number of ships
         * - Checks ships disposition don't overlap or go outside the grid.
         *
         * @param playerSocket The HeartbeatClient that manages the player socket
         * @return The list of Ships. In case of unknown error, will return null.
         */
        private List<Ship> setupGrid(HeartbeatClient playerSocket) {
            String grid;
            String[] inputShips;
            boolean isGridOk;
            List<Ship> parsedShips = null;

            try {
                // GET grid and checkGrid() -> if Ok, try parseGrid() -> if Ok, continue to game
                isGridOk = false;
                do {
                    playerSocket.println("SEND_GRID");

                    // Wait for message
                    while (!playerSocket.available()) {
                        if (playerSocket.isClosed()) {
                            return null;
                        }
                        if (opponent.playerSocket.isClosed()) {
                            playerSocket.println("WIN_OPPONENT_DC");
                            return null;
                        }

                        Thread.sleep(10);
                    }

                    grid = playerSocket.getOneMessage();

                    inputShips = grid.split("_");

                    if (inputShips.length != NUM_SHIPS || !checkLenOfAllElements(inputShips, 7) || !checkShipsFormat(inputShips)) {
                        playerSocket.println("GRID_ERR");
                    } else {
                        // PARSE Grid
                        // call parseGrid only after initial format checking
                        parsedShips = parseGrid(inputShips);
                        if (parsedShips == null) {
                            playerSocket.println("GRID_ERR");
                        }
                        isGridOk = true;
                    }

                } while (!isGridOk);

                playerSocket.println("GRID_OK");
                return parsedShips;

            } catch (IllegalStateException | InterruptedException e) {
                e.printStackTrace();
            }

            // if unknown error occurs
            return null;
        }

        /**
         * Parse the input grid string to a List of Ship after checking if the grid is valid.
         *
         * @param inputShips the array of ships received from user input
         * @return A list with all the ships if the grid is valid, otherwise null
         */
        private List<Ship> parseGrid(String[] inputShips) throws IllegalArgumentException {
            if (inputShips == null || inputShips.length != NUM_SHIPS)
                throw new IllegalArgumentException("Invalid input ships array");

            try {
                List<Ship> ships = new ArrayList<>();
                List<String> tmpShipCells;
                // cellState has the values: 0=(water), 1=(ship | waterNextToShip). Ship's shouldn't be placed on cellState 1
                boolean[][] tempGrid = new boolean[GRID_SIZE][GRID_SIZE];
                int column, row, length;
                char orientation;

                for (String ship : inputShips) {
                    column = Integer.parseInt(ship.substring(0, 2)) - 1;
                    row = Integer.parseInt(ship.substring(2, 4)) - 1;
                    length = Integer.parseInt(ship.substring(5, 7));
                    orientation = ship.charAt(4);
                    tmpShipCells = new ArrayList<>();

                    // marks cells where ships can't be placed
                    switch (orientation) {
                        case 'H':
                            // check all ship's cells are placeable
                            for (int i = 0; i < length; i++) {
                                if (tempGrid[row][column + i])
                                    return null;
                            }

                            // updates cell state
                            for (int i = 0; i < length; i++) {
                                // blocks all cells apart from the next one on the right
                                for (int dx = (row > 0 ? -1 : 0); dx <= (row < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                                    for (int dy = (column + i > 0 ? -1 : 0); dy <= (column + i < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                        if (dx != 0 || dy != 1) {
                                            tempGrid[row + dx][column + i + dy] = true;
                                        }
                                    }
                                }
                                tmpShipCells.add(String.format("%02d%02d", (row + 1), (column + 1 + i)));
                            }
                            // set last cell on the right
                            if (column + length < GRID_SIZE) {
                                tempGrid[row][column + length] = true;
                            }
                            break;
                        case 'V':
                            // check all ship's cells are placeable
                            for (int i = 0; i < length; i++) {
                                if (tempGrid[row + i][column])
                                    return null;
                            }

                            // updates cell state
                            for (int i = 0; i < length; i++) {
                                // blocks all cells apart from the next one on the bottom
                                for (int dx = (row + i > 0 ? -1 : 0); dx <= (row + i < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                                    for (int dy = (column > 0 ? -1 : 0); dy <= (column < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                        if (dx != 1 || dy != 0) {
                                            tempGrid[row + i + dx][column + dy] = true;
                                        }
                                    }
                                }
                                tmpShipCells.add(String.format("%02d%02d", (row + 1 + i), (column + 1)));
                            }
                            // set last cell on the bottom
                            if (row + length < GRID_SIZE) {
                                tempGrid[row + length][column] = true;
                            }
                            break;
                    }

                    ships.add(new Ship(tmpShipCells));
                }

                return ships;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }

        private boolean checkShipsFormat(String[] ships) {
            if (ships == null) return false;

            try {
                List<Integer> availableShips = new ArrayList<>(AVAILABLE_SHIPS); // list of available ships
                char orientation;
                int row, column;
                Integer length;

                // check same number of ships
                if (ships.length != availableShips.size())
                    return false;

                for (String ship : ships) {
                    // check ship length available
                    length = Integer.parseInt(ship.substring(5, 7));
                    if (!availableShips.remove(length))
                        return false;

                    // check orientation
                    orientation = ship.charAt(4);
                    if (orientation != 'H' && orientation != 'V')
                        return false;

                    // check coordinates
                    column = Integer.parseInt(ship.substring(0, 2));
                    row = Integer.parseInt(ship.substring(2, 4));
                    if (row < 1 || row > GRID_SIZE || (orientation == 'V' && row > (GRID_SIZE + 1 - length)))
                        return false;
                    if (column < 1 || column > GRID_SIZE || (orientation == 'H' && column > (GRID_SIZE + 1 - length)))
                        return false;
                }

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private boolean checkLenOfAllElements(String[] arr, int len) {
            if (arr == null) return false;

            for (String elem : arr) {
                if (elem.length() != len)
                    return false;
            }
            return true;
        }

        public void setOpponent(Player opponent) throws IllegalArgumentException {
            if (opponent == null)
                throw new IllegalArgumentException("opponent is null");

            this.opponent = opponent;
        }
    }
}
