package Server.Common;

import java.io.*;
import java.net.*;

public class TCPConnection {
  private String serverHost;
  private int serverPort;
  private String serverName;


  public TCPConnection(String host, int port, String name) throws IOException,UnknownHostException {
      serverHost = host;
      serverPort = port;
      serverName = name;
  }

  public String sendCommand(String cmd) throws IOException {
    String serverResponse = "";
    String line;
    Socket clientSocket = new Socket(serverHost, serverPort);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    outToServer.writeBytes(cmd + "\n");
    while ( (line = inFromServer.readLine()) != null ) {
      serverResponse += line;
    }
    System.out.println(serverResponse); // TODO: Delete
    clientSocket.close();

    return serverResponse;
  }
}
