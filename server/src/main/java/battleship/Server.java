package battleship;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Server {
    public static void main(String[] args) {
        //TODO: Read configs from external file (like threadPoolSize)

        ExecutorService pool = Executors.newFixedThreadPool(50); // Max numbers of player (sockets)
        ArrayList<Socket> queue = new ArrayList<>();

        try {
            ServerSocket listener = new ServerSocket(12345);
            System.out.println("[*] ServerSocket created, listening for connections...");

            // Queue manager
            //TODO: Implement queue menager

            while (true) {
                //TODO: If a player connect and disconnect a place will be occupied and the other player will be alone
                //TODO: If a player disconnect while playing inform the other user and end the match
                queue.add(listener.accept());
            }
        } catch (IOException e) {
            System.out.println("[!] An exception was thrown while creating ServerSocket");
            e.printStackTrace();
        }

        Runnable playerMatcher = new Runnable() {
            @Override
            public void run() {

            }
        };
    }
}
