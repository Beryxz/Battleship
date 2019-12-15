package battleship;

/**
 * Class used to return also the sankShip when status if SANK
 */
public class ShotResult {
    private Shot status;
    private Ship sankShip;

    public ShotResult(Shot status, Ship sankShip) {
        this.status = status;
        this.sankShip = sankShip;
    }

    public ShotResult(Shot status) {
        this.status = status;
        this.sankShip = null;
    }

    public Shot getStatus() {
        return status;
    }

    public Ship getSankShip() {
        return sankShip;
    }
}
