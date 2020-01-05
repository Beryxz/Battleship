package battleship;

import battleship.heartbeat.HeartbeatClient;
import battleship.heartbeat.HeartbeatManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.RejectedExecutionException;

public class Server {
    private static final int GAMESERVER_PORT = 12345;
    private static final int MAX_CONNECTIONS = 50;

    public static void main(String[] args) {
        //TODO: Read configs from external file (like threadPoolSize)
        try {
            QueueManager queueManager = new QueueManager();
            HeartbeatManager heartbeatManager = new HeartbeatManager(2000);

            // Socket listener
            ServerSocket listener = new ServerSocket(GAMESERVER_PORT);
            System.out.println("[*] Listening for connections on port: " + GAMESERVER_PORT);

            while (true) {
                try {
                    HeartbeatClient c = new HeartbeatClient(listener.accept(), 1000);
                    System.out.println(String.format("[*] '%s' connected", c.getSocketInfo()));
                    heartbeatManager.add(c);
                    queueManager.add(c);
                } catch (IllegalAccessException e) {
                    System.out.println("[!] The listener returned an empty socket");
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("[!] An exception was thrown while creating ServerSocket");
            e.printStackTrace();
        } catch (RejectedExecutionException e) {
            System.out.println("[!] An exception was thrown while creating the QueueManager");
            e.printStackTrace();
        }
    }

}
