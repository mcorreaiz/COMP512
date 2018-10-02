package Client;

import Server.Interface.*;

import java.net.InetAddress;
import java.net.Socket;

import java.util.*;
import java.io.*;

public class TCPClient extends Client
{
	private static String s_serverHost = "0.0.0.0";
	private static int s_serverPort = 3000;
	// private static String s_serverName = "Server";
	public Socket socket;

	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group9";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			s_serverPort = args[1];
		}
		if (args.length > 2)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.TCPClient [server_hostname [server_port]]");
			System.exit(1);
		}

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		try {
	    m_resourceManager = new TCPClient(
	            InetAddress.getByName(s_serverHost),
	            s_serverPort);
	    System.out.println("\r\nConnected to Server: " + m_resourceManager.socket.getInetAddress());
	    m_resourceManager.start();
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public TCPClient(InetAddress serverAddress, int serverPort) throws Exception {
		super();
		this.socket = new Socket(serverAddress, serverPort);
	}

	// public void connectServer()
	// {
	// 	connectServer(s_serverHost, s_serverPort, s_serverName);
	// }
	//
	// public void connectServer(String server, int port, String name)
	// {
	// 	try {
	// 		boolean first = true;
	// 		while (true) {
	// 			try {
	// 				Registry registry = LocateRegistry.getRegistry(server, port);
	// 				m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
	// 				System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
	// 				break;
	// 			}
	// 			catch (NotBoundException|RemoteException e) {
	// 				if (first) {
	// 					System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
	// 					first = false;
	// 				}
	// 			}
	// 			Thread.sleep(500);
	// 		}
	// 	}
	// 	catch (Exception e) {
	// 		System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
	// 		e.printStackTrace();
	// 		System.exit(1);
	// 	}
	// }
}
