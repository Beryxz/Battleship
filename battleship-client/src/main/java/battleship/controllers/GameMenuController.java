package battleship.controllers;

import battleship.util.PlayerSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GameMenuController implements Initializable {
    public GridPane trackingGrid;
    public GridPane targetGrid;
    public ImageView turnBulb;

    private PlayerSocket gsSocket;
    private Thread ioThread;
    private boolean ourTurn;
    private List<Cell> shootsHistory;
    private Cell lastShoot;

    public GameMenuController(final PlayerSocket gsSocket) throws IllegalArgumentException {
        if (gsSocket == null) {
            throw new IllegalArgumentException("gsSocket is null");
        }

        this.gsSocket = gsSocket;
    }

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        // Set initial state bulb
        this.turnBulb.setImage(new Image("img/bulb_red_off.png"));
        this.ourTurn = false;

        this.shootsHistory = new ArrayList<>();

        // Set default blank Pane
        for (int i = 0; i < trackingGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < trackingGrid.getColumnConstraints().size(); j++) {
                Pane p = new Pane();
                this.trackingGrid.add(p, i, j);
                p.setMaxHeight(25);
                p.setPrefHeight(25);
                p.setMaxWidth(25);
                p.setPrefWidth(25);
            }
        }
        for (int i = 0; i < targetGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < targetGrid.getColumnConstraints().size(); j++) {
                Pane p = new Pane();
                this.targetGrid.add(p, i, j);
                p.setMaxHeight(30);
                p.setPrefHeight(30);
                p.setMaxWidth(30);
                p.setPrefWidth(30);
            }
        }

        // Center icons
        for (ColumnConstraints n :
                trackingGrid.getColumnConstraints()) {
            n.setHalignment(HPos.CENTER);
        }
        for (ColumnConstraints n :
                targetGrid.getColumnConstraints()) {
            n.setHalignment(HPos.CENTER);
        }

        // TODO remove test code
        ioThread = new Thread(() -> {
            waitOpponent(this.gsSocket);
            this.gsSocket.getOut().println("0202V05_0204H02_0404H02_0604H04_0207H01_0407H01_0409V03");
            play(this.gsSocket);
        });
        ioThread.start();
    }

    public void clickGrid(MouseEvent mouseEvent) {
        Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
        if (clickedNode != targetGrid) {
            // click on descendant node
            Integer colIndex = GridPane.getColumnIndex(clickedNode),
                    rowIndex = GridPane.getRowIndex(clickedNode);

            Cell cell = new Cell(String.format("%02d%02d", rowIndex + 1, colIndex + 1), colIndex, rowIndex);

            if (ourTurn && !shootsHistory.contains(cell)) {
                shootsHistory.add(cell);
                lastShoot = cell;
                this.gsSocket.getOut().println("SHOOT_" + cell.coordinates);
            }
        }
    }


    private void play(final PlayerSocket gsSocket) {
        boolean gameStarted = false;

        while (gsSocket.getIn().hasNextLine()) {
            String msg = gsSocket.getIn().nextLine();
            System.out.println("Server: " + msg);

            if (!gameStarted) {
                if (msg.equals("GAME_START"))
                    gameStarted = true;
            } else {
                if (!this.ourTurn) {
                    if (msg.equals("TURN_START")) {
                        ourTurn = true;
                        Platform.runLater(() -> {
                            this.turnBulb.setImage(new Image("img/bulb_red_on.png"));
                        });
                    } else if (msg.startsWith("HIT_")) {
                        Platform.runLater(() -> {
                            ImageView iv = new ImageView("img/hit.png");
                            iv.setFitWidth(25);
                            iv.setFitHeight(25);
                            this.trackingGrid.add(iv, Integer.parseInt(msg.substring(6, 8)) - 1, Integer.parseInt(msg.substring(4, 6)) - 1);
                        });
                    } else if (msg.startsWith("OCEAN_")) {
                        Platform.runLater(() -> {
                            ImageView iv = new ImageView("img/ocean.png");
                            iv.setFitWidth(25);
                            iv.setFitHeight(25);
                            this.trackingGrid.add(iv, Integer.parseInt(msg.substring(8, 10)) - 1, Integer.parseInt(msg.substring(6, 8)) - 1);
                        });
                    } else if (msg.startsWith("SANK_")) {
                        Platform.runLater(() -> {
                            for (String ship : msg.substring(5).split("_")) {
                                ImageView iv = new ImageView("img/sank.png");
                                iv.setFitWidth(25);
                                iv.setFitHeight(25);
                                this.trackingGrid.add(iv, Integer.parseInt(ship.substring(2, 4)) - 1, Integer.parseInt(ship.substring(0, 2)) - 1);
                            }
                        });
                    } else if (msg.startsWith("LOST")) {
                        //TODO
                    }
                } else {
                    if (msg.equals("TURN_END")) {
                        ourTurn = false;
                        Platform.runLater(() -> {
                            this.turnBulb.setImage(new Image("img/bulb_red_off.png"));
                        });
                    } else if (msg.equals("HIT")) {
                        Platform.runLater(() -> {
                            ImageView iv = new ImageView("img/hit.png");
                            iv.setFitWidth(30);
                            iv.setFitHeight(30);
                            this.targetGrid.add(iv, lastShoot.colIndex, lastShoot.rowIndex);
                        });
                    } else if (msg.equals("OCEAN")) {
                        Platform.runLater(() -> {
                            ImageView iv = new ImageView("img/ocean.png");
                            iv.setFitWidth(30);
                            iv.setFitHeight(30);
                            this.targetGrid.add(iv, lastShoot.colIndex, lastShoot.rowIndex);
                        });
                    } else if (msg.startsWith("SANK_")) {
                        Platform.runLater(() -> {
                            for (String ship : msg.substring(5).split("_")) {
                                ImageView iv = new ImageView("img/sank.png");
                                iv.setFitWidth(30);
                                iv.setFitHeight(30);
                                this.targetGrid.add(iv, Integer.parseInt(ship.substring(2, 4)) - 1, Integer.parseInt(ship.substring(0, 2)) - 1);
                            }
                        });
                    } else if (msg.startsWith("WIN")) {
                        //TODO
                    }
                }

            }
        }
    }

    /**
     * Returns when "OPPONENT_FOUND" message is received on the PlayerSocket input stream.
     *
     * @param gsSocket the game server socket
     */
    private void waitOpponent(final PlayerSocket gsSocket) {
        while (gsSocket.getIn().hasNextLine()) {
            String msg = gsSocket.getIn().nextLine();
            if (msg.equals("OPPONENT_FOUND"))
                return;
        }
    }

    public void dispose() {
        ioThread.interrupt();
    }

    private class Cell {
        public String coordinates;
        public int colIndex, rowIndex;

        public Cell(String coordinates, int colIndex, int rowIndex) {
            this.coordinates = coordinates;
            this.colIndex = colIndex;
            this.rowIndex = rowIndex;
        }
    }
}
