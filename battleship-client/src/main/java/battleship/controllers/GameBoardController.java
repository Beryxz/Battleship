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
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.ResourceBundle;

public class GameBoardController implements Initializable {
    public GridPane playerGrid;
    public GridPane opponentGrid;
    public ImageView turnBulb;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        // Set initial state bulb
        this.turnBulb.setImage(new Image("img/bulb_red_off.png"));

        // Set default blank Pane
        for (int i = 0; i < opponentGrid.getRowConstraints().size(); i++) {
            for (int j = 0; j < opponentGrid.getColumnConstraints().size(); j++) {
                Pane p = new Pane();
                this.opponentGrid.add(p, i, j);
                p.setMaxHeight(30);
                p.setPrefHeight(30);
                p.setMaxWidth(30);
                p.setPrefWidth(30);
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
            System.out.println(String.format("Mouse clicked cell: %02d%02d", rowIndex + 1, colIndex + 1));

            // TODO set image based on server response
            ImageView iv = new ImageView("img/hit.png");
            this.opponentGrid.add(iv, colIndex, rowIndex);
            iv.setFitWidth(30);
            iv.setFitHeight(30);
        }
    }
}
