package battleship;

/**
 * Type of shot responses that are returned after a player shoot the opponent
 */
public enum Shot {
    HIT, // A ship was partially destroyed
    SANK, // A ship was utterly destroyed
    OCEAN, // Nothing hit
    DUPLICATE // Shot already thrown
}
