package battleship;

import battleship.controllers.ShipPlacementMenuController;
import battleship.util.PlayerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class Main extends Application {
    /**
     * Gameserver Socket
     */
    private PlayerSocket gsSocket;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Stage set-up
            gsSocket = new PlayerSocket(new Socket("127.0.0.1", 12345));

            FXMLLoader shipPlacementMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/shipPlacementMenu.fxml")));
            shipPlacementMenuLoader.setControllerFactory(aClass -> new ShipPlacementMenuController(gsSocket));
            primaryStage.setOnCloseRequest(windowEvent -> {
                try {
                    gsSocket.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Platform.exit();
                System.exit(0);
            });
            Parent shipPlacementMenu = shipPlacementMenuLoader.load();
            primaryStage.setTitle("Battleship");
            primaryStage.setScene(new Scene(shipPlacementMenu, 650, 400));
            primaryStage.setResizable(false);
            primaryStage.show();

            //TODO Start on mainMenu scene
            //TODO Ask server ip and port, wait for opponent and ask for ships layout

        } catch (IOException e) {
            System.out.println("Couldn't connect to gameserver. Check connection parameters.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    /**
//     * Returns when "OPPONENT_FOUND" message is received on the PlayerSocket input stream.
//     *
//     * @param gsSocket the game server socket
//     */
//    private void waitOpponent(final PlayerSocket gsSocket) {
//        while (gsSocket.getIn().hasNextLine()) {
//            String msg = gsSocket.getIn().nextLine();
//            if (msg.equals("OPPONENT_FOUND"))
//                return;
//        }
//    }
}