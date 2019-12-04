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
        private Ship[] ships;

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
            String[] ships;
            boolean isGridOk = true;

            try {
                // GET grid and checkGrid()
                do {
                    out.println("SEND_GRID");
                    if (in.hasNextLine()) {
                        grid = in.nextLine();
                        ships = grid.split("_");

                        if (ships.length != NUM_SHIPS || !checkLenOfAllElements(ships, 7) || !checkShipsFormat(ships)) {
                            isGridOk = false;
                            out.println("GRID_ERR");
                        }
                    }

                } while (!isGridOk);

                // PARSE grid
                out.println("GRID_OK");


            } catch (IllegalStateException e) {
                e.printStackTrace();
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
                    if (x < 1 || x > GRID_SIZE || (orientation == 'H' && x > (GRID_SIZE + 1 - length)))
                        return false;
                    if (y < 1 || y > GRID_SIZE || (orientation == 'V' && y > (GRID_SIZE + 1 - length)))
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
