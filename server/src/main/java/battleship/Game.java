package battleship;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * TODO: Write Grid & Ship Format. What user should send ecc.
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

    private Player currentPlayer;

    public Game() {
        this.currentPlayer = null;
    }

    class Player implements Runnable {
        private PlayerSocket playerSocket;
        private Scanner in;
        private PrintWriter out;
        private List<Ship> ships;

        private Player opponent;

        public Player(PlayerSocket playerSocket) {
            this.playerSocket = playerSocket;
            this.in = playerSocket.getIn();
            this.out = playerSocket.getOut();
        }

        @Override
        public void run() {
            try {
                // grid disposition
                setupGrid();

                // game start
                //TODO: Wait for opponent

                while (true) {
                    if (in.nextLine().equals("q"))
                        return;
                }
            } catch (NoSuchElementException e) {
                // Also catches SIGINT
            } finally {
                try {
                    System.out.println(String.format("[*] '%s' disconnected", playerSocket.getSocket().getLocalSocketAddress().toString()));
                    playerSocket.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Checks that grid from input is formatted correctly.
         * - Checks number of ships sent
         * - Checks format of each one
         * - Checks there's the right number of ships
         * - Checks ships disposition don't overlap or go outside the grid.
         */
        private void setupGrid() {
            String grid;
            String[] inputShips;
            boolean isGridOk;
            List<Ship> parsedShips;

            try {
                // GET grid and checkGrid() -> if Ok, try parseGrid() -> if Ok, continue to game
                do {
                    isGridOk = true;
                    out.println("SEND_GRID");
                    if (in.hasNextLine()) {
                        grid = in.nextLine();
                        inputShips = grid.split("_");

                        if (inputShips.length != NUM_SHIPS || !checkLenOfAllElements(inputShips, 7) || !checkShipsFormat(inputShips)) {
                            isGridOk = false;
                            out.println("GRID_ERR");
                        } else {
                            // PARSE Grid
                            // call parseGrid only after initial format checking
                            parsedShips = parseGrid(inputShips);
                            if (parsedShips == null) {
                                isGridOk = false;
                                out.println("GRID_ERR");
                            }
                            this.ships = parsedShips;
                        }
                    }

                } while (!isGridOk);

                out.println("GRID_OK");

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        /**
         * Parse the input grid string to a List of Ship after checking if the grid is valid.
         *
         * @param inputShips the array of ships received from user input
         * @return A list with all the ships if the grid is valid, otherwise null
         */
        private List<Ship> parseGrid(String[] inputShips) {
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
            for (String elem : arr) {
                if (elem.length() != len)
                    return false;
            }
            return true;
        }

        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }
    }
}
