package battleship.controllers;

import battleship.util.PlayerSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class GameMenuController implements Initializable {
    @FXML
    public GridPane trackingGrid;
    @FXML
    public GridPane targetGrid;
    @FXML
    public ImageView turnBulb;
    @FXML
    public Label infoLabel;
    @FXML
    public Pane endDialog;
    @FXML
    public Button backMainMenu;
    @FXML
    public Text length1remained;
    @FXML
    public Text length2remained;
    @FXML
    public Text length3remained;
    @FXML
    public Text length4remained;
    @FXML
    public Text length5remained;

    final private PlayerSocket gsSocket;
    private boolean ourTurn;
    private List<Cell> shootsHistory;
    private Cell lastShoot;
    private String playerGrid;
    private int[] availableShipsLengths = {2, 2, 1, 1, 1};

    public GameMenuController(final PlayerSocket gsSocket, final String playerGrid) throws IllegalArgumentException {
        if (gsSocket == null) {
            throw new IllegalArgumentException("gsSocket is null");
        }

        this.gsSocket = gsSocket;
        this.playerGrid = playerGrid;
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
                p.setPrefSize(25, 25);
                p.setMaxSize(25, 25);
                this.trackingGrid.add(p, i, j);
            }
        }
        for (int i = 0; i < targetGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < targetGrid.getColumnConstraints().size(); j++) {
                Pane p = new Pane();
                p.setPrefSize(30, 30);
                p.setMaxSize(30, 30);
                this.targetGrid.add(p, i, j);
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

        // Set ships in trackingGrid
        for (String shipCell : playerGrid.split("_")) {
            int i = Integer.parseInt(shipCell.substring(2, 4)) - 1,
                    j = Integer.parseInt(shipCell.substring(0, 2)) - 1;
            char orientation = shipCell.charAt(4);

            for (int iLen = 0; iLen < Integer.parseInt(shipCell.substring(5, 7)); iLen++) {
                Pane p = new Pane();
                p.setPrefSize(25, 25);
                p.setMaxSize(25, 25);
                p.getStyleClass().add("ship");
                if (orientation == 'H') {
                    this.trackingGrid.add(p, j + iLen, i);
                } else {
                    this.trackingGrid.add(p, j, i + iLen);
                }
            }
        }

        // Set remained ships count
        updateRemainedShips();

        // Events Handlers
        backMainMenu.setOnMouseClicked(mouseEvent -> {
            try {
                gsSocket.getSocket().close();
                switchToMainMenu();
            } catch (IOException e) {
                e.printStackTrace();
                Platform.exit();
            }
        });

        for (Node cell :
                targetGrid.getChildren()) {

            cell.setOnMouseEntered(mouseEvent -> {
                if (ourTurn)
                    cell.setStyle("-fx-background-color: #bb86fc");
            });

            cell.setOnMouseExited(mouseEvent -> {
                cell.setStyle(null);
            });
        }

        // Start game
        new Thread(() -> play(this.gsSocket)).start();
    }

    /**
     * Updates GUI remained count based on local array 'availableShipsLengths'
     */
    private void updateRemainedShips() {
        length1remained.setText(String.valueOf(availableShipsLengths[0]));
        length2remained.setText(String.valueOf(availableShipsLengths[1]));
        length3remained.setText(String.valueOf(availableShipsLengths[2]));
        length4remained.setText(String.valueOf(availableShipsLengths[3]));
        length5remained.setText(String.valueOf(availableShipsLengths[4]));
    }

    /**
     * Change stage to "MainMenu"
     */
    private void switchToMainMenu() {
        try {
            // Get primaryStage
            Stage stage = (Stage) trackingGrid.getScene().getWindow();

            // Load gameMenu
            FXMLLoader mainMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/mainMenu.fxml")));
            Parent mainMenu = mainMenuLoader.load();
            stage.setTitle("Battleship");
            // Switch stages
            stage.setScene(new Scene(mainMenu, 400, 400));
            stage.setResizable(false);
            stage.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void play(final PlayerSocket gsSocket) {
        while (gsSocket.getIn().hasNextLine()) {
            String msg = gsSocket.getIn().nextLine();

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
                        for (String shipCell : msg.substring(5).split("_")) {
                            int columnIndex = Integer.parseInt(shipCell.substring(2, 4)) - 1;
                            int rowIndex = Integer.parseInt(shipCell.substring(0, 2)) - 1;
                            Pane p = new Pane();
                            p.setPrefSize(25, 25);
                            p.setMaxSize(25, 25);
                            this.trackingGrid.add(p, columnIndex, rowIndex);
                            ImageView iv = new ImageView("img/sank.png");
                            iv.setFitWidth(25);
                            iv.setFitHeight(25);
                            this.trackingGrid.add(iv, columnIndex, rowIndex);
                        }
                    });
                } else if (msg.startsWith("LOST")) {
                    Platform.runLater(() -> {
                        // Show remained ships
                        for (String cell : msg.substring(5).split("_")) {
                            int columnIndex = Integer.parseInt(cell.substring(2)) - 1,
                                    rowIndex = Integer.parseInt(cell.substring(0, 2)) - 1;
                            this.targetGrid.getChildren().get(columnIndex * 10 + rowIndex).getStyleClass().add("ship");
                        }

                        infoLabel.setText("You Lost!");
                        endDialog.setVisible(true);
                    });
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
                        String[] shipCells = msg.substring(5).split("_");

                        for (String shipCell : shipCells) {
                            int columnIndex = Integer.parseInt(shipCell.substring(2, 4)) - 1;
                            int rowIndex = Integer.parseInt(shipCell.substring(0, 2)) - 1;
                            Pane p = new Pane();
                            p.setPrefSize(30, 30);
                            p.setMaxSize(30, 30);
                            this.targetGrid.add(p, columnIndex, rowIndex);
                            ImageView iv = new ImageView("img/sank.png");
                            iv.setFitWidth(30);
                            iv.setFitHeight(30);
                            this.targetGrid.add(iv, columnIndex, rowIndex);
                        }

                        // Update remained ships count
                        availableShipsLengths[shipCells.length - 1]--;
                        updateRemainedShips();
                    });
                } else if (msg.startsWith("WIN")) {
                    Platform.runLater(() -> {
                        infoLabel.setText("You Win!");
                        endDialog.setVisible(true);
                    });
                }
            }
        }
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
