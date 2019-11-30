package battleship;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.*;

public class QueueManager {
    private static final int QUEUE_MANAGER_DELAY = 2000; // ms to wait before checking again when <2 players in queue

    private LinkedList<PlayerSocket> queue;
    private ScheduledExecutorService queueManager;
    private GamesManager gamesManager;

    /**
     * Starts the queue manager task that joins 2 players in a match.
     * Queue has no size limit other than system resources limitation.
     *
     * @throws RejectedExecutionException If there's an error with the queue maintainer task
     */
    public QueueManager() throws RejectedExecutionException {
        this.queue = new LinkedList<>();
        this.gamesManager = new GamesManager();

        try {
            queueManager = Executors.newSingleThreadScheduledExecutor();
            queueManager.scheduleWithFixedDelay(playerMatcher, 1000, QUEUE_MANAGER_DELAY, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            throw new RejectedExecutionException("QueueManager scheduled task was rejected");
        }
    }

    private Runnable playerMatcher = () -> {
        System.out.println("[*] Player in queue: " + length());
        while (length() >= 2) {
            CompletableFuture.runAsync(() -> gamesManager.create(queue.pop(), queue.pop()));
        }
    };

    /**
     * Adds the passed socket to the queue
     * @param player Player socket
     * @throws IllegalAccessException If socket is null
     * @throws IOException If PlayerSocket streams couldn't be obtained
     */
    public void add(Socket player) throws IllegalAccessException, IOException {
        if (player == null) {
            throw new IllegalAccessException("Player socket is null");
        }

        PlayerSocket playerSocket = new PlayerSocket(player);
        queue.add(playerSocket);
        playerSocket.getOut().println("WAIT_OPPONENT");
    }

    public int length() {
        return queue.size();
    }
}