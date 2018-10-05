// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.TCP;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPResourceManager extends ResourceManager
{
	private static String s_serverName = "Server";
	private static int s_serverPort = 2905;
	private ServerSocket serverSocket;
	private static String connectMsg = "ConnectionTest";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}

		if (args.length > 1) {
			// It's CustomerRM. Initialize other RM's stubs.
			String[] names = {"Cars", "Flights", "Rooms"};
			s_resourceManagers = new HashMap();
			try {
				for (int i = 1; i < 4; i++) {
					//connect to 3 RMs
					boolean first = true;
					while (true) {
						try {
							System.out.println("Trying to connect to " + names[i-1]);
							System.out.println(args[i]);
							TCPConnection testConnection = new TCPConnection(args[i], s_serverPort, names[i-1]);
							String response = testConnection.sendCommand(connectMsg);
							//store the connection if successful
							my_serverPort = s_serverPort;
							s_resourceManagers.put(names[i-1],args[i]);
							System.out.println("Connected to '" + names[i-1] + "' server [" + args[i] + ":" + s_serverPort + "/" + names[i-1] + "]");
							break;
						}
						catch (Exception e) {
							System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
							e.printStackTrace();
							if (first) {
								System.out.println("Waiting for '" + names[i] + "' server [" + args[i] + ":" + s_serverPort + "/" + names[i] + "]");
								first = false;
							}
						}
						Thread.sleep(500);
					}
				}
			}
			catch (Exception e) {
				System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Create the TCP server instance
		try {
			// Create a new Server object
			TCPResourceManager server = new TCPResourceManager(s_serverName);
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_serverName + "'");
			ServerSocket welcomeSocket = new ServerSocket(s_serverPort);

			while (true)
			{
				Socket connectionSocket = welcomeSocket.accept();
				TCPServerRM thread = new TCPServerRM(connectionSocket, server);
				thread.start();
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public TCPResourceManager(String name) throws Exception
	{
		super(name);
	}

	private void listen() throws Exception {
			String data = null;
			Socket client = this.serverSocket.accept();
			String clientAddress = client.getInetAddress().getHostAddress();
			System.out.println("\r\nNew connection from " + clientAddress);

			BufferedReader in = new BufferedReader(
							new InputStreamReader(client.getInputStream()));
			while ( (data = in.readLine()) != null ) {
					System.out.println("\r\nMessage from " + clientAddress + ": " + data);
			}
	}

	public InetAddress getSocketAddress() {
			return this.serverSocket.getInetAddress();
	}

	public int getPort() {
			return this.serverSocket.getLocalPort();
	}
}
