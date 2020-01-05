package battleship;

import battleship.heartbeat.HeartbeatClient;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueManager {
    private static final int QUEUE_MANAGER_DELAY = 2000; // ms to wait before checking again when <2 players in queue

    private LinkedList<HeartbeatClient> queue;
    private ScheduledExecutorService queueManager;
    private ScheduledExecutorService statusPrinter;
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
            statusPrinter = Executors.newSingleThreadScheduledExecutor();
            statusPrinter.scheduleWithFixedDelay(printStatus, 1, 10, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            throw new RejectedExecutionException("QueueManager scheduled task was rejected");
        }
    }

    private Runnable playerMatcher = () -> {
        // check to update player in queue when there's only 1 player
        if (length() == 1) {
            if (queue.getFirst().isClosed())
                queue.pop();
        }
        else {
            while (length() >= 2) {
                // With large amount of requests this may slow down the queueManager since it's run with one thread and create() is blocking
                HeartbeatClient ps1 = queue.pop();
                if (ps1.isClosed())
                    continue;

                HeartbeatClient ps2 = queue.pop();
                if (ps2.isClosed()) {
                    queue.push(ps1);
                    continue;
                }

                ps1.println("OPPONENT_FOUND");
                ps2.println("OPPONENT_FOUND");

                gamesManager.create(ps1, ps2);
            }
        }
    };

    private Runnable printStatus = () -> {
        System.out.println("[*] Player in queue: " + length() + "; n. threads: " + ManagementFactory.getThreadMXBean().getThreadCount());
    };

    /**
     * Adds the passed socket to the queue
     *
     * @param player Player socket
     * @throws IllegalAccessException If socket is null
     */
    public void add(HeartbeatClient player) throws IllegalAccessException {
        if (player == null)
            throw new IllegalAccessException("Player socket is null");

        queue.add(player);
        player.println("OPPONENT_WAIT");
    }

    public int length() {
        return queue.size();
    }
}
