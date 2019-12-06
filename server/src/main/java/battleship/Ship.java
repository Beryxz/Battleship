package battleship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ship {
    private final List<String> squares;
    private int length;
    private List<String> squaresHit;

    /**
     * Create a ship
     * @param squares array of "XXYY" positions of the ship placement
     * @throws IllegalArgumentException if ships is invalid
     */
    public Ship(String[] squares) throws IllegalArgumentException {
        if (squares == null || squares.length < 1)
            throw new IllegalArgumentException("squares is invalid");

        this.squares = Arrays.asList(squares);
        this.length = squares.length;
        this.squaresHit = new ArrayList<>();
    }

    /**
     * Create a ship
     * @param squares List of "XXYY" positions of the ship placement
     * @throws IllegalArgumentException if ships is invalid
     */
    public Ship(List<String> squares) throws IllegalArgumentException {
        if (squares == null || squares.size() < 1)
            throw new IllegalArgumentException("squares is invalid");

        this.squares = squares;
        this.length = squares.size();
        this.squaresHit = new ArrayList<>();
    }

    /**
     * Tries to hit the ship and if succeed, will update the ship status.
     * @param hitPosition "XXYY" position to hit.
     * @return The appropriate Shot.* response for how the hit affected the ship.
     */
    public Shot hit(String hitPosition) throws IllegalArgumentException {
        if (hitPosition == null || hitPosition.length() != 4)
            throw new IllegalArgumentException("hitPosition argument is invalid.");

        if (squares.contains(hitPosition)) {
            if (!squaresHit.contains(hitPosition)) {
                squaresHit.add(hitPosition);
                return squaresRemained() == 0 ? Shot.SANK : Shot.HIT ;
            }
            return Shot.DUPLICATE;
        } else {
            return Shot.OCEAN;
        }
    }

    public int squaresRemained() {
        return this.length - squaresHit.size();
    }
}
