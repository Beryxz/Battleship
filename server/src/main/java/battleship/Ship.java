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
     * Tries to hit the ship, if shot succeed. the hit will be saved and number of squaresRemained will be returned.
     * Otherwise is going to be returned -1 when ship is missed and -2 when hit has already been counted.
     * If 0 is returned, ship is sank.
     * @param hitPosition "XXYY" position to hit
     * @return true if hit, false otherwise
     */
    public int hit(String hitPosition) {
        if (squares.contains(hitPosition)) {
            if (!squaresHit.contains(hitPosition)) {
                squaresHit.add(hitPosition);
                return squaresRemained(); // hit
            }
            return -2; // already hit
        } else {
            return -1; // missed
        }
    }

    public int squaresRemained() {
        return this.length - squaresHit.size();
    }
}
