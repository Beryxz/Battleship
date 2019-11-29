package battleship;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;

public class Server {
    private static final int GAMESERVER_PORT = 12345;
    private static final int MAX_CONNECTIONS = 50;

    public static void main(String[] args) {
        //TODO: Read configs from external file (like threadPoolSize)
        try {
            QueueManager queueManager = new QueueManager(MAX_CONNECTIONS);

            // Socket listener
            ServerSocket listener = new ServerSocket(GAMESERVER_PORT);
            System.out.println("[*] Listening for connections on port: " + GAMESERVER_PORT);

            while (true) {
                //TODO: If a player disconnects while in the queue it isn't removed (heartbeat / keep-alive)
                try {
                    Socket s = listener.accept();
                    //TODO: Let player choose a name before joining the queue
                    System.out.println(String.format("[*] '%s' connected", s.getLocalSocketAddress().toString()));
                    queueManager.add(s);
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
