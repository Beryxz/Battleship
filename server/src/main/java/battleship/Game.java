package battleship;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Game {
    //TODO: If a player disconnects while in game, the other should be warned

    private Player currentPlayer;

    public Game() {
        this.currentPlayer = null;
    }

    class Player implements Runnable {
        private PlayerSocket playerSocket;
        private Scanner in;
        private PrintWriter out;

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
                out.println("SEND_GRID");

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

        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }
    }
}
