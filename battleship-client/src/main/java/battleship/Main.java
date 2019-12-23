package battleship;

import battleship.controllers.MainMenuController;
import battleship.util.PlayerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
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
            FXMLLoader mainMenuLoader = new FXMLLoader(Objects.requireNonNull(getClass().getClassLoader().getResource("views/mainMenu.fxml")));
            mainMenuLoader.setControllerFactory(aClass -> new MainMenuController(gsSocket));
            primaryStage.setOnCloseRequest(windowEvent -> {
                try {
                    if (gsSocket != null)
                        gsSocket.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Platform.exit();
                System.exit(0);
            });

            Parent mainMenu = mainMenuLoader.load();
            primaryStage.setTitle("Battleship");
            primaryStage.setScene(new Scene(mainMenu, 400, 400));
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