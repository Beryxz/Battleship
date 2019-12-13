package battleship;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Scanner;

public class Client extends Application {
    /**
     * Gameserver socket
     */
    private Socket gsSocket;
    private Scanner in;
    private PrintWriter out;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Stage set-up
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("views/shipPlacement.fxml")));

        primaryStage.setTitle("Battleship - Game");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.setResizable(false);
        primaryStage.show();


        // Game set-up
        // Player set-up
        //TODO: ask server ip and port to the end user
        try {
            this.gsSocket = new Socket("127.0.0.1", 12345);
            this.in = new Scanner(this.gsSocket.getInputStream());
            this.out = new PrintWriter(this.gsSocket.getOutputStream(), true);

            waitOpponent(this.gsSocket);

            //setupGrid();

            //TODO Main menu in which to setup name and ships layout
            play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitOpponent(final Socket gsSocket) {
        //TODO
    }

    private void play() {
        //TODO
    }
}
