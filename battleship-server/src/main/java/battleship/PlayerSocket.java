package battleship;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Class that initializes in & out streams given a socket
 */
public class PlayerSocket {
    private Scanner in;
    private PrintWriter out;
    private Socket socket;

    /**
     * Initialize in & out streams. Out stream has autoFlush
     * @param socket Target socket
     * @throws IOException If something went wrong while retrieving in, out streams from socket
     * @throws IllegalArgumentException If socket is null
     */
    public PlayerSocket(Socket socket) throws IOException, IllegalArgumentException {
        if (socket == null)
            throw new IllegalArgumentException("Socket is null");

        this.socket = socket;
        this.in = new Scanner(socket.getInputStream());
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public Scanner getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }
}
