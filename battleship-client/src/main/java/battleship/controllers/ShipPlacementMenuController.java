package battleship.controllers;

import battleship.util.PlayerSocket;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

public class ShipPlacementMenuController implements Initializable {
    @FXML
    public GridPane trackingGrid;

    @FXML
    public Button confirmGrid;
    @FXML
    public Button clearGrid;

    @FXML
    public ToggleGroup ships;
    @FXML
    public RadioButton length1;
    @FXML
    public RadioButton length2;
    @FXML
    public RadioButton length3;
    @FXML
    public RadioButton length4;
    @FXML
    public RadioButton length5;

    @FXML
    public ToggleGroup shipOrientation;
    @FXML
    public ToggleButton orientationH;
    @FXML
    public ToggleButton orientationV;
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

    public static final int GRID_SIZE = 10;
    public static final int[] shipsLengths = {2, 2, 1, 1, 1};

    private int[] availableShipsLengths;
    private List<String> shipsCells;
    private PlayerSocket gsSocket;

    /**
     * cellState has the values: 0=(water), 1=(ship | waterNextToShip). Ship's shouldn't be placed on cellState 1
     */
    private boolean[][] placeableGrid = new boolean[GRID_SIZE][GRID_SIZE];

    public ShipPlacementMenuController(final PlayerSocket gsSocket) {
        this.gsSocket = gsSocket;
        this.shipsCells = new ArrayList<>();
        this.availableShipsLengths = shipsLengths.clone();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set default blank Pane
        for (int i = 0; i < trackingGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < trackingGrid.getColumnConstraints().size(); j++) {
                Pane p = new Pane();
                this.trackingGrid.add(p, i, j);
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

        // Select initial button states
        length1.setSelected(true);
        orientationH.setSelected(true);

        // Set initial remained ships count
        updateRemainedShipsAndRadioButtons();

        // Events handler
        for (Node n : trackingGrid.getChildren()) {
            n.setOnMouseEntered((MouseEvent me) -> {
                setAllCells(n, "-fx-background-color: yellow", "-fx-background-color: red");
            });
            n.setOnMouseExited((MouseEvent me) -> {
                setAllCells(n, null, null);
            });

            // Event handler for actual ship placement
            n.setOnMouseClicked((MouseEvent me) -> {
                Toggle selectedRadioShipLength = ships.getSelectedToggle();
                if (selectedRadioShipLength == null)
                    return;

                int cell = trackingGrid.getChildren().indexOf(n),
                        shipLength = Integer.parseInt(((RadioButton) selectedRadioShipLength).getText());

                if (!isShipAvailable(shipLength))
                    return;

                String newShipInfo = String.format("%02d%02d%s%02d",
                        (int) Math.ceil(cell / 10.0), // xx
                        ((cell - 1) % 10) + 1, // yy
                        (shipOrientation.getSelectedToggle() == orientationH) ? "H" : "V", // Orientation
                        shipLength // Length
                );

                if (checkAndUpdateGrid(newShipInfo)) {
                    System.out.println(newShipInfo);
                    shipsCells.add(newShipInfo);

                    // updates remained ships
                    availableShipsLengths[shipLength - 1] -= 1;
                    updateRemainedShipsAndRadioButtons();
                    if (isGridSubmittable()) {
                        confirmGrid.setDisable(false);
                    }

                    // Color cells
                    ObservableList<Node> cells = trackingGrid.getChildren();
                    int selectedCell = cells.indexOf(n);
                    int orientation = (shipOrientation.getSelectedToggle() == orientationH) ? 10 : 1; // 10='H', 1='V'
                    Node tmpCell;

                    // color ship in the grid
                    for (int i = 0; i < shipLength; i++) {
                        tmpCell = cells.get(selectedCell + i * orientation);
                        tmpCell.setStyle(null);
                        tmpCell.getStyleClass().add("ship");
                    }
                }
            });
        }

        clearGrid.setOnMouseClicked(mouseEvent -> {
            // disable grid submit
            confirmGrid.setDisable(true);

            // remove 'ship' style class from nodes
            trackingGrid.getChildren().forEach(node -> node.getStyleClass().remove("ship"));

            // reset available ships
            availableShipsLengths = shipsLengths.clone();
            updateRemainedShipsAndRadioButtons();

            // remove set ships
            shipsCells.clear();

            // reset placeable grid
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    placeableGrid[i][j] = false;
                }
            }
        });

        confirmGrid.setOnMouseClicked(mouseEvent -> {
            // disable double click
            confirmGrid.setDisable(true);

            // Send grid
            String grid = String.join("_", shipsCells);
            System.out.println(grid);
            gsSocket.getOut().println(grid);

            // Server response
            String response = gsSocket.getIn().nextLine();
            if (response.equals("GRID_OK")) {
                // Change stage to "GameMenu"
                try {
                    // Get primaryStage
                    Stage stage = (Stage) trackingGrid.getScene().getWindow();

                    // Load gameMenu
                    FXMLLoader gameMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/gameMenu.fxml")));
                    gameMenuLoader.setControllerFactory(aClass -> new GameMenuController(gsSocket));
                    Parent gameMenu = gameMenuLoader.load();
                    stage.setTitle("Battleship - Game");
                    // Set on exit resources disposal
                    stage.setOnCloseRequest(windowEvent -> {
                        gameMenuLoader.<GameMenuController>getController().dispose();
                         try {
                            gsSocket.getSocket().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Platform.exit();
                        System.exit(0);
                    });
                    // Switch stages
                    stage.setScene(new Scene(gameMenu, 600, 600));
                    stage.setResizable(false);
                    stage.sizeToScene();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("Server returned GRID_ERR of a supposed valid grid");
            }
        });
    }

    private boolean isGridSubmittable() {
        for (int remained :
                availableShipsLengths) {
            if (remained != 0)
                return false;
        }

        return true;
    }

    private boolean isShipAvailable(int length) {
        return length >= 1 && length <= availableShipsLengths.length && availableShipsLengths[length - 1] >= 1;
    }

    private void updateRemainedShipsAndRadioButtons() {
        length1remained.setText(String.valueOf(availableShipsLengths[0]));
        if (availableShipsLengths[0] == 0) {
            length1.setDisable(true);
            length1.setSelected(false);
        } else {
            length1.setDisable(false);
        }

        length2remained.setText(String.valueOf(availableShipsLengths[1]));
        if (availableShipsLengths[1] == 0) {
            length2.setDisable(true);
            length2.setSelected(false);
        } else {
            length2.setDisable(false);
        }

        length3remained.setText(String.valueOf(availableShipsLengths[2]));
        if (availableShipsLengths[2] == 0) {
            length3.setDisable(true);
            length3.setSelected(false);
        } else {
            length3.setDisable(false);
        }

        length4remained.setText(String.valueOf(availableShipsLengths[3]));
        if (availableShipsLengths[3] == 0) {
            length4.setDisable(true);
            length4.setSelected(false);
        } else {
            length4.setDisable(false);
        }

        length5remained.setText(String.valueOf(availableShipsLengths[4]));
        if (availableShipsLengths[4] == 0) {
            length5.setDisable(true);
            length5.setSelected(false);
        } else {
            length5.setDisable(false);
        }
    }

    /**
     * Sets in the 'trackingGrid' grid, the cells of the ship to the corresponding placeable color
     *
     * @param shipNode              The initial node of the ship
     * @param cellStylePlaceable    css style to apply if placeable
     * @param cellStyleNotPlaceable css style to apply if not placeable
     */
    public void setAllCells(Node shipNode, String cellStylePlaceable, String cellStyleNotPlaceable) {
        Toggle selectedRadioShipLength = ships.getSelectedToggle();
        if (selectedRadioShipLength == null)
            return;

        ObservableList<Node> cells = trackingGrid.getChildren();
        Node tmpCell;
        int selectedCell = cells.indexOf(shipNode),
                shipLength = Integer.parseInt(((RadioButton) selectedRadioShipLength).getText()),
                x,
                y;
        int orientation = (shipOrientation.getSelectedToggle() == orientationH) ? 10 : 1; // 10='H', 1='V'

        if ((orientation == 10 && (selectedCell + (shipLength - 1) * orientation) <= 100) || // horizontal overflow check
                (orientation == 1 && ((selectedCell - 1 + shipLength - 1) % 10) >= (selectedCell - 1) % 10)) // vertical overflow check
        {
            x = ((selectedCell - 1) % 10);
            y = (int) Math.ceil(selectedCell / 10.0) - 1;

            // check if ship is placeable
            if (orientation == 10) { // 'H'
                for (int i = 0; i < shipLength; i++) {
                    if (placeableGrid[x][y + i]) {
                        shipNode.setStyle(cellStyleNotPlaceable);
                        return;
                    }
                }
            } else { // 'V'
                for (int i = 0; i < shipLength; i++) {
                    if (placeableGrid[x + i][y]) {
                        shipNode.setStyle(cellStyleNotPlaceable);
                        return;
                    }
                }
            }

            for (int i = 0; i < shipLength; i++) {
                tmpCell = cells.get(selectedCell + i * orientation);

                if (cellStylePlaceable == null || !tmpCell.getStyleClass().contains("ship"))
                    tmpCell.setStyle(cellStylePlaceable);
            }
        } else {
            shipNode.setStyle(cellStyleNotPlaceable);
        }
    }

    /**
     * Tries to add the new ship to the local object grid 'placeableGrid' and check if the grid it's still valid.
     * If the grid is valid adds the new ship to the local grid and return true.
     * Otherwise 'placeableGrid' isn't modified and False is returned.
     *
     * @param newShipInfo the new ship to verify
     * @return True if the grid is valid after adding newShip.
     */
    private boolean checkAndUpdateGrid(String newShipInfo) {
        try {
            int x, y, length;
            char orientation;

            y = Integer.parseInt(newShipInfo.substring(0, 2)) - 1;
            x = Integer.parseInt(newShipInfo.substring(2, 4)) - 1;
            length = Integer.parseInt(newShipInfo.substring(5, 7));
            orientation = newShipInfo.charAt(4);

            // marks cells where ships can't be placed
            switch (orientation) {
                case 'H':
                    // check ship doesn't overflow borders
                    if (y + length > GRID_SIZE)
                        return false;

                    // check if ship is placeable
                    for (int i = 0; i < length; i++) {
                        if (placeableGrid[x][y + i]) {
                            return false;
                        }
                    }

                    // update cells status
                    for (int i = 0; i < length; i++) {
                        // blocks all cells apart from the next one on the right
                        for (int dx = (x > 0 ? -1 : 0); dx <= (x < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                            for (int dy = (y + i > 0 ? -1 : 0); dy <= (y + i < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                if (dx != 0 || dy != 1) {
                                    placeableGrid[x + dx][y + i + dy] = true;
                                }
                            }
                        }
                    }
                    // set last cell on the right
                    if (y + length < GRID_SIZE) {
                        placeableGrid[x][y + length] = true;
                    }
                    break;
                case 'V':
                    // check ship doesn't overflow borders
                    if (x + length > GRID_SIZE)
                        return false;

                    // check if ship is placeable
                    for (int i = 0; i < length; i++) {
                        if (placeableGrid[x + i][y]) {
                            return false;
                        }
                    }

                    // update cells status
                    for (int i = 0; i < length; i++) {
                        // blocks all cells apart from the next one on the bottom
                        for (int dx = (x + i > 0 ? -1 : 0); dx <= (x + i < GRID_SIZE - 1 ? 1 : 0); ++dx) {
                            for (int dy = (y > 0 ? -1 : 0); dy <= (y < GRID_SIZE - 1 ? 1 : 0); ++dy) {
                                if (dx != 1 || dy != 0) {
                                    placeableGrid[x + i + dx][y + dy] = true;
                                }
                            }
                        }
                    }
                    // set last cell on the bottom
                    if (x + length < GRID_SIZE) {
                        placeableGrid[x + length][y] = true;
                    }
                    break;
            }

            return true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

}
