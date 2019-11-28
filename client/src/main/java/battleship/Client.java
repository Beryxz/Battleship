package battleship;

public class Client {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("[!] Missing <server_ip> <port> as arguments");
        }
    }
}
