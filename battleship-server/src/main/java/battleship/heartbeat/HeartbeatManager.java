package battleship.heartbeat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that manages all Sockets and controls they are still online checking, every specified period of time. if they are still in use.
 */
public class HeartbeatManager {
    /**
     * Timeout in milliseconds after which a client is considered disconnected
     */
    private long msDisconnectTimeout;
    /**
     * The Runnable task that checks if all the clients are still up
     */
    private ScheduledExecutorService heartbeatAgent;
    /**
     * List of all the clients that are checked
     */
    private List<HeartbeatClient> clients;

    /**
     * Initialize the manager and start the scheduled checker task every msDisconnectTimeout/1.5 milliseconds.
     * @param msDisconnectTimeout The time in milliseconds after which a client is considered disconnected
     * @throws IllegalArgumentException If msDisconnectedTimeout is less or equal to zero
     */
    public HeartbeatManager(long msDisconnectTimeout) throws IllegalArgumentException {
        if (msDisconnectTimeout <= 0) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        this.msDisconnectTimeout = msDisconnectTimeout;

        clients = new ArrayList<>();

        heartbeatAgent = Executors.newSingleThreadScheduledExecutor();
        heartbeatAgent.scheduleAtFixedRate(checkClientsBeatsTask, 0, (long) Math.ceil(msDisconnectTimeout / 1.5), TimeUnit.MILLISECONDS);
    }

    private Runnable checkClientsBeatsTask = () -> {
        try {
            long currentTime = System.currentTimeMillis();

            for (HeartbeatClient client : clients) {
                if (currentTime - client.getLastBeat() > msDisconnectTimeout) {
                    client.disconnect();
                    clients.remove(client);
                }
            }
        } catch (Exception ignore) {
        }
    };

    /**
     * Adds a Client to the heartbeat monitor list
     * @param c Client to add
     */
    public void add(HeartbeatClient c) {
        clients.add(c);
    }
}
