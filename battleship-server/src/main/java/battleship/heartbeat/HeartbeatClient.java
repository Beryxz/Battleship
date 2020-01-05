package battleship.heartbeat;

import battleship.util.PlayerSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client based on PlayerSocket class with the addition of functions used to check is the client is still alive.
 */
public class HeartbeatClient {
    private PlayerSocket client;
    private long lastBeat;
    private boolean isClosed;
    /**
     * Thread that manages the input stream
     */
    private ExecutorService iThread;
    /**
     * Buffer that stores received messages and prevent blocking requests
     */
    private LinkedList<String> inBuffer;

    /**
     * Initialize the PlayerSocket class with the given Socket and set lastBeat to currentTime
     *
     * @param client               Client socket
     * @param initialTimeoutOffset time in milliseconds added to the currentTime to ensure stability on initial connection Setup
     * @throws IllegalArgumentException If client socket is null or initialTimeoutOffset is less than zero
     * @throws IOException              If there was an error creating the PlayerSocket
     */
    public HeartbeatClient(Socket client, long initialTimeoutOffset) throws IllegalArgumentException, IOException {
        if (client == null) {
            throw new IllegalArgumentException("Client socket is null");
        }
        if (initialTimeoutOffset < 0) {
            throw new IllegalArgumentException("initialTimeoutOffset is less than zero");
        }

        this.client = new PlayerSocket(client);
        this.lastBeat = System.currentTimeMillis() + initialTimeoutOffset;
        this.isClosed = false;
        this.inBuffer = new LinkedList<>();

        // Thread that manages the input stream
        iThread = Executors.newSingleThreadExecutor();
        iThread.execute(iThreadTask);
    }

    /**
     * Destructor that properly close threads and sockets
     *
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            client.getSocket().close();
            iThread.shutdownNow();
            this.clearBuffer();
        } finally {
            super.finalize();
        }
    }

    private Runnable iThreadTask = () -> {
        while (!isClosed) {
            try {
                int available = this.client.getSocket().getInputStream().available();
                if (available > 0) {
                    String msg = this.client.getIn().nextLine();

                    this.beat();

                    if (!msg.startsWith("PING")) {
                        inBuffer.add(msg);

                    }
                }
                Thread.sleep(10);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    };

    public long getLastBeat() {
        return lastBeat;
    }

    /**
     * Print a message in the output stream
     *
     * @param msg The message to be printed
     * @throws IllegalArgumentException If argument is null
     */
    public void println(String msg) throws IllegalArgumentException {
        client.println(msg);
    }

    /**
     * Checks if there are messages in the input buffer
     *
     * @return true if there at least one message in the input buffer
     */
    public boolean available() {
        return inBuffer.size() > 0;
    }

    /**
     * Gets one message from the input buffer
     *
     * @return A string if there's a message to be returned, null otherwise
     */
    public String getOneMessage() {
        if (available()) {
            return inBuffer.pop();
        } else {
            return null;
        }
    }

    /**
     * Removes all the messages in the input buffer
     */
    public void clearBuffer() {
        inBuffer.clear();
    }

    /**
     * Returns whether or not, the disconnect() function has been called on this object.
     *
     * @return true if disconnect() function has been called(), false otherwise.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Updates lastBeat with currentTime.
     */
    private void beat() {
        lastBeat = System.currentTimeMillis();
    }

    /**
     * Close client socket and send a disconnect message to System.out stream
     */
    public void disconnect() {
        try {
            if (!isClosed) {
                System.out.println(String.format("[*] '%s' disconnected", getSocketInfo()));
                client.getSocket().close();
                iThread.shutdownNow();
                this.isClosed = true;
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * Gets remote Address and port of the current client socket
     * @return Socket address and port formatted into a string
     */
    public String getSocketInfo() {
        return this.client.getSocket().getRemoteSocketAddress().toString();
    }
}
