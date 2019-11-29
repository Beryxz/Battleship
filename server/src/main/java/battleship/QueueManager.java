package battleship;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueManager {
    private static final int QUEUE_MANAGER_DELAY = 1;

    private LinkedList<PlayerSocket> queue;
    private ScheduledExecutorService queueManager;
    private GamesManager gamesManager;

    /**
     * Starts the queue manager task that joins 2 players in a match
     *
     * @param maxParallelGames Maximum numbers of simultaneous games
     * @throws RejectedExecutionException If there's an error with the queue maintainer task
     */
    public QueueManager(int maxParallelGames) throws RejectedExecutionException {
        this.queue = new LinkedList<>();
        this.gamesManager = new GamesManager(maxParallelGames);

        try {
            queueManager = Executors.newSingleThreadScheduledExecutor();
            queueManager.scheduleWithFixedDelay(playerMatcher, 5, QUEUE_MANAGER_DELAY, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            throw new RejectedExecutionException("QueueManager scheduled task was rejected");
        }
    }

    private Runnable playerMatcher = () -> {
        if (length() >= 2) {
            gamesManager.create(queue.pop(), queue.pop());
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

        queue.add(new PlayerSocket(player));
    }

    public int length() {
        return queue.size();
    }
}