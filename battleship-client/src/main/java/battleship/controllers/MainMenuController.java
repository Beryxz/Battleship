package battleship.controllers;

import battleship.util.PlayerSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class MainMenuController implements Initializable {
    @FXML
    public TextField serverIp;
    @FXML
    public TextField serverPort;
    @FXML
    public Pane waitOpponent;
    @FXML
    public Button connectBtn;
    @FXML
    public Text errorTxt;

    private PlayerSocket gsSocket;

    public MainMenuController() {}

    public PlayerSocket getGsSocket() {
        return gsSocket;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectBtn.setOnMouseClicked(mouseEvent -> {
            connectBtn.setDisable(true);
            errorTxt.setVisible(false);

            try {
                // Try to connect to server
                Socket sock = new Socket(serverIp.getText(), Integer.parseInt(serverPort.getText(), 10));

                // Wait for opponent
                waitOpponent.setVisible(true);
                gsSocket = new PlayerSocket(sock);

                CompletableFuture
                        .runAsync(() -> waitGridRequest(gsSocket))
                        .thenRun(() -> {
                            Platform.runLater(this::switchToShipPlacementMenu);
                        });
            } catch (Exception e) {
                errorTxt.setVisible(true);
                connectBtn.setDisable(false);
                gsSocket = null;
                return;
            }
        });
    }

    /**
     * Change stage to "ShipPlacementMenu"
     */
    private void switchToShipPlacementMenu() {
        try {
            // Get primaryStage
            Stage stage = (Stage) connectBtn.getScene().getWindow();

            // Load gameMenu
            FXMLLoader shipPlacementMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/shipPlacementMenu.fxml")));
            shipPlacementMenuLoader.setControllerFactory(aClass -> new ShipPlacementMenuController(gsSocket));
            Parent shipPlacementMenu = shipPlacementMenuLoader.load();
            stage.setTitle("Battleship - Ship placement");
            // Set on exit resources disposal
            stage.setOnCloseRequest(windowEvent -> {
                try {
                    gsSocket.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Platform.exit();
                System.exit(0);
            });
            // Switch stages
            stage.setScene(new Scene(shipPlacementMenu, 650, 400));
            stage.setResizable(false);
            stage.sizeToScene();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns when "SEND_GRID" message is received on the PlayerSocket input stream.
     * To receive SEND_GRID, the server has to match the player to an opponent
     *
     * @param gsSocket the game server socket
     */
    private void waitGridRequest(final PlayerSocket gsSocket) {
        while (gsSocket.getIn().hasNextLine()) {
            String msg = gsSocket.getIn().nextLine();
            if (msg.equals("SEND_GRID"))
                return;
        }
    }
}
