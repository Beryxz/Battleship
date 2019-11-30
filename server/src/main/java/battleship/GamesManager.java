package battleship;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

class GamesManager {
    private ExecutorService gamesPool;

    public GamesManager() {
        // 2 players per game
        this.gamesPool = Executors.newCachedThreadPool();
    }

    public void create(PlayerSocket socketP1, PlayerSocket socketP2) throws IllegalArgumentException {
        if (socketP1 == null || socketP2 == null) {
            throw new IllegalArgumentException("Player socket is null");
        }

        // Join players in the same game
        Game game = new Game();
        Game.Player p1 = game.new Player(socketP1);
        Game.Player p2 = game.new Player(socketP2);
        gamesPool.execute(p1);
        gamesPool.execute(p2);
        // Set-up game
        p1.setOpponent(p2);
        p2.setOpponent(p1);
    }
}
