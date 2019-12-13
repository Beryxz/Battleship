package battleship;

import battleship.util.PlayerSocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // Set-up game
        p1.setOpponent(p2);
        p2.setOpponent(p1);
        game.currentPlayer = p1;

        // Start game
        //TODO Threads aren't instantly closed when run() returns
        gamesPool.execute(p1);
        gamesPool.execute(p2);
    }
}
