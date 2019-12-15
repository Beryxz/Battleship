package battleship;

import battleship.controllers.GameMenuController;
import battleship.util.PlayerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

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
            gsSocket = new PlayerSocket(new Socket("127.0.0.1", 12345));

            // Stage set-up
            FXMLLoader gameMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/gameMenu.fxml")));
            gameMenuLoader.setControllerFactory(aClass -> new GameMenuController(gsSocket));
            Parent gameMenu = gameMenuLoader.load();
//        Parent mainMenu = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("views/mainMenu.fxml")));

            //TODO Start on mainMenu scene
            //TODO Ask server ip and port, wait for opponent and ask for ships layout
            primaryStage.setTitle("Battleship - Game");
            primaryStage.setOnCloseRequest(windowEvent -> {
                gameMenuLoader.<GameMenuController>getController().dispose();
                Platform.exit();
                System.exit(0);
            });
            primaryStage.setScene(new Scene(gameMenu, 600, 600));
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (IOException e) {
            System.out.println("Couldn't connect to gameserver. Check connection parameters.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}