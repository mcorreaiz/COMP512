package Client;

import java.io.*;
import java.net.*;

class TCPClient {
  public String serverResponse;
  public Socket clientSocket;
  public DataOutputStream outToServer;
  public BufferedReader inFromServer ;

  // public static void main(String argv[]) throws Exception {
  //   String sentence;
  //   String modifiedSentence;
  //   InetAddress serverHost = InetAddress.getByName("cs-9.cs.mcgill.ca");
  //   int serverPort = 3000;
  //   Socket clientSocket = new Socket(serverHost, serverPort);
  //
  //   DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
  //   BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  //
  //   outToServer.writeBytes("hi \n");
  //   modifiedSentence = inFromServer.readLine();
  //   System.out.println("FROM SERVER: " + modifiedSentence);
  //   clientSocket.close();
  // }

  public TCPClient(String serverHost, int serverPort, String name) throws IOException,UnknownHostException {
    System.out.println("Connected to '" + name + "' server [" + serverHost + ":" + serverPort + "]");
    Socket clientSocket = new Socket(serverHost, serverPort);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  }

  public boolean sendCommand(String cmd) throws IOException {
    outToServer.writeBytes(cmd + "\n");
    serverResponse = inFromServer.readLine();
    System.out.println("FROM SERVER: " + serverResponse);
    clientSocket.close();
    return true;
  }
}
