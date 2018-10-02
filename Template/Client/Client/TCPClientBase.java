// Code taken from https://www.pegaxchange.com/2017/12/07/simple-tcp-ip-server-client-java/

package Client;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class TCPClientBase {
    private Socket socket;
    private Scanner scanner;
    private TCPClientBase(InetAddress serverAddress, int serverPort) throws Exception {
        this.socket = new Socket(serverAddress, serverPort);
        this.scanner = new Scanner(System.in);
    }
    private void start() throws IOException {
        String input;
        while (true) {
            input = scanner.nextLine();
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
            out.println(input);
            out.flush();
        }
    }

    public static void main(String[] args) throws Exception {
        TCPClientBase client = new TCPClientBase(
                InetAddress.getByName(args[0]),
                Integer.parseInt(args[1]));

        System.out.println("\r\nConnected to Server: " + client.socket.getInetAddress());
        client.start();
    }
}
