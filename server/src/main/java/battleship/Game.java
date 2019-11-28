package battleship;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class Game {
    class Player implements Runnable {
        private Socket socket;

        Player(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("[*] It Works!");
                while (true) {
                    if (in.nextLine().equals("q"))
                        return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ignored) {
                // Catches SIGINT
            } finally {
                try {
                    System.out.println(String.format("[*] '%s' disconnected", socket.getLocalSocketAddress().toString()));
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
