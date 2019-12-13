package battleship;

import battleship.util.PlayerSocket;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * TODO: Write Grid & Ship Format. What user should send ecc. (Works only for two players)
 * XXYYHLL
 * xx column
 * yy row
 * [HV] orientation
 * LL length
 */
public class Game {
    //TODO: If a player disconnects while in game, the other should be warned

    public static final int NUM_SHIPS = 7;
    public static final int GRID_SIZE = 10;
    public static final List<Integer> AVAILABLE_SHIPS = Arrays.asList(1, 1, 2, 2, 3, 4, 5);

    private CountDownLatch playersReady = new CountDownLatch(2);
    public Player currentPlayer;

    public Game() {
        this.currentPlayer = null;
    }

    class Player implements Runnable {
        private PlayerSocket playerSocket;
        private Scanner in;
        private PrintWriter out;
        private List<Ship> ships;
        private Player opponent;
        private List<String> shotHistory;
        private List<String> opponentShotHistory;

        public Player(PlayerSocket playerSocket) {
            this.playerSocket = playerSocket;
            this.in = playerSocket.getIn();
            this.out = playerSocket.getOut();
            this.shotHistory = new ArrayList<>();
            this.opponentShotHistory = new ArrayList<>();
        }

        @Override
        public void run() {
            int x, y;
            String nextLine, shot;

            try {
                // grid disposition
                do {
                    ships = setupGrid(in, out);
                } while (ships == null);
                playersReady.countDown();
                playersReady.await();

                // game start
                out.println("GAME_START");
                // initial TURN_START
                if (currentPlayer == this)
                    out.println("TURN_START");

                //TODO: hasNextLine is blocking, when a player disconnect with ^C, it's not elaborated
                while (in.hasNextLine()) {
                    nextLine = in.nextLine();

                    // Check if it's not player's turn
                    if (currentPlayer != this) {
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
                            out.println("INVALID");
                            continue;
                        }

                        // check if shot was already thrown in this game
                        if (this.shotHistory.contains(shot)) {
                            out.println("DUPLICATE");
                            continue;
                        }

                        this.shotHistory.add(shot);

                        switch (this.opponent.shoot(shot)) {
                            case HIT:
                                out.println("HIT");
                                opponent.out.println("HIT_" + shot);
                                break;
                            case OCEAN:
                                out.println("OCEAN");
                                opponent.out.println("OCEAN_" + shot);
                                break;
                            case SANK:
                                out.println("SANK");
                                opponent.out.println("SANK_" + shot);
                                break;
                        }

                        // check if player Won the game
                        if (opponent.hasLost()) {
                            out.println("WIN");
                            opponent.out.println("LOST");
                            break;
                        }

                        // End turn and pass the game to the next player
                        currentPlayer = opponent;
                        out.println("TURN_END");
                        opponent.out.println("TURN_START");
                    } else {
                        out.println("INVALID");
                        continue;
                    }
                }
            } catch (IllegalStateException | NoSuchElementException e) {
                // Input Scanner closed or SIGINT
                opponent.out.println("WIN_OPPONENT_DC");
            } catch (InterruptedException e) {
                // Catches error while awaiting game ready state
                e.printStackTrace();
            } finally {
                try {
                    System.out.println(String.format("[*] '%s' disconnected", playerSocket.getSocket().getLocalSocketAddress().toString()));
                    opponent.playerSocket.getSocket().close();
                    this.playerSocket.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Thread.currentThread().interrupt();
            return;
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
        private Shot shoot(String coordinates) throws IllegalArgumentException {
            if (coordinates == null || coordinates.length() != 4)
                throw new IllegalArgumentException("Invalid coordinates");

            opponentShotHistory.add(coordinates);

            for (Ship s : ships) {
                switch (s.hit(coordinates)) {
                    case OCEAN:
                        continue;
                    case HIT:
                        return Shot.HIT;
                    case SANK:
                        ships.remove(s);
                        return Shot.SANK;
                    case DUPLICATE:
                        return Shot.DUPLICATE;
                }
            }

            // if no ship was hit, then it was a miss
            return Shot.OCEAN;
        }

        /**
         * Get grid from "in" and checks that is formatted correctly.
         * - Checks number of ships sent
         * - Checks format of each one
         * - Checks there's the right number of ships
         * - Checks ships disposition don't overlap or go outside the grid.
         *
         * @param in  the player input socket stream Scanner
         * @param out the player output socket stream PrintWriter
         * @return The list of Ships. In case of unknown error, will return null.
         */
        private List<Ship> setupGrid(Scanner in, PrintWriter out) {
            String grid;
            String[] inputShips;
            boolean isGridOk;
            List<Ship> parsedShips = null;

            try {
                // GET grid and checkGrid() -> if Ok, try parseGrid() -> if Ok, continue to game
                isGridOk = false;
                do {
                    out.println("SEND_GRID");
                    if (in.hasNextLine()) {
                        grid = in.nextLine();
                        inputShips = grid.split("_");

                        if (inputShips.length != NUM_SHIPS || !checkLenOfAllElements(inputShips, 7) || !checkShipsFormat(inputShips)) {
                            out.println("GRID_ERR");
                        } else {
                            // PARSE Grid
                            // call parseGrid only after initial format checking
                            parsedShips = parseGrid(inputShips);
                            if (parsedShips == null) {
                                out.println("GRID_ERR");
                            }
                            isGridOk = true;
                        }
                    }

                } while (!isGridOk);

                out.println("GRID_OK");
                return parsedShips;

            } catch (IllegalStateException e) {
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
                int x, y, length;
                char orientation;

                for (String ship : inputShips) {
                    x = Integer.parseInt(ship.substring(0, 2)) - 1;
                    y = Integer.parseInt(ship.substring(2, 4)) - 1;
                    length = Integer.parseInt(ship.substring(5, 7));
                    orientation = ship.charAt(4);
                    tmpShipCells = new ArrayList<>();

                    // marks cells where ships can't be placed
                    switch (orientation) {
                        case 'H':
                            for (int i = 0; i < length; i++) {
                                if (tempGrid[x][y + i])
                                    return null;

                                // blocks all cells apart from the next one on the right
                                for (int dx = (x > 0 ? -1 : 0); dx <= (x < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                                    for (int dy = (y + i > 0 ? -1 : 0); dy <= (y < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                        if (dx != 0 || dy != 1) {
                                            tempGrid[x + dx][y + i + dy] = true;
                                        }
                                    }
                                }
                                tmpShipCells.add(String.format("%02d%02d", (x + 1), (y + 1 + i)));
                            }
                            // set last cell on the right
                            tempGrid[x][y + length] = true;
                            break;
                        case 'V':
                            for (int i = 0; i < length; i++) {
                                if (tempGrid[x + i][y])
                                    return null;

                                // blocks all cells apart from the next one on the bottom
                                for (int dx = (x + i > 0 ? -1 : 0); dx <= (x < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                                    for (int dy = (y > 0 ? -1 : 0); dy <= (y < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                        if (dx != 1 || dy != 0) {
                                            tempGrid[x + i + dx][y + dy] = true;
                                        }
                                    }
                                }
                                tmpShipCells.add(String.format("%02d%02d", (x + 1 + i), (y + 1)));
                            }
                            // set last cell on the bottom
                            tempGrid[x + length][y] = true;
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
                int x, y;
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
                    x = Integer.parseInt(ship.substring(0, 2));
                    y = Integer.parseInt(ship.substring(2, 4));
                    if (x < 1 || x > GRID_SIZE || (orientation == 'V' && x > (GRID_SIZE + 1 - length)))
                        return false;
                    if (y < 1 || y > GRID_SIZE || (orientation == 'H' && y > (GRID_SIZE + 1 - length)))
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
