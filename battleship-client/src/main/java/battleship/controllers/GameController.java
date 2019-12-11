package battleship.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    @FXML
    public GridPane playerGrid;
    @FXML
    public GridPane opponentGrid;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        this.opponentGrid.setAlignment(Pos.CENTER);

        opponentGrid.getStyleClass().add("grid-pane");

        // Set default ocean icon
        for (int i = 0; i < opponentGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < opponentGrid.getColumnConstraints().size(); j++) {
                ImageView iv = new ImageView("img/ocean.png");
                this.opponentGrid.add(iv, i, j);
                iv.setFitWidth(30);
                iv.setFitHeight(30);
            }
        }

        // Center icons
        for (ColumnConstraints n :
                opponentGrid.getColumnConstraints()) {
            n.setHalignment(HPos.CENTER);
        }
    }

    public void clickGrid(MouseEvent mouseEvent) {
        Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
        if (clickedNode != opponentGrid) {
            // click on descendant node
            Integer colIndex = GridPane.getColumnIndex(clickedNode),
                    rowIndex = GridPane.getRowIndex(clickedNode);
            System.out.println("Mouse clicked cell: " + colIndex + ", " + rowIndex);
        }
    }
}
