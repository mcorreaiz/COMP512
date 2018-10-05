package Server.TCP;

import Server.Interface.*;
import Server.Common.*;

import java.util.*;
import java.io.*;
import java.net.*;



public class TCPMiddleware extends Middleware
{
	private static String s_serverName = "Middleware";
	private static int serverPort = 2905;
	private static String connectMsg = "ConnectionTest";


	public static void main(String args[])
	{
		if (args.length > 3) {
			String[] names = {"Cars", "Flights", "Rooms", "Customers"};
			rm_connections = new HashMap();
			try {
				System.out.println("testing out TCP connections to resource managers");
				for (int i = 0; i < 4; i++) {
					//connect to 4 RMs
					boolean first = true;
					while (true) {
						try {
							System.out.println("Trying to connect to " + names[i]);
							TCPConnection testConnection = new TCPConnection(args[i],serverPort,names[i]);
							String response = testConnection.sendCommand(connectMsg);
							//store the connection if successful
							my_serverPort = serverPort;
							rm_connections.put(names[i],args[i]);
							System.out.println("Connected to '" + names[i] + "' server [" + args[i] + ":" + serverPort + "/" + names[i] + "]");
							break;
						}
						catch (Exception e) {
							System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
							e.printStackTrace();
							if (first) {
								System.out.println("Waiting for '" + names[i] + "' server [" + args[i] + ":" + serverPort + "/" + names[i] + "]");
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
		else{
			System.out.println("Only received " + args.length + " RM locations");
			throw new IllegalArgumentException("Middleware must know about 4 other RM! missing RM");
		}

		// start serving
		try
		{
			// Create a new middleware object
			TCPMiddleware middle = new TCPMiddleware(s_serverName);
			middle.start();
			ServerSocket welcomeSocket = new ServerSocket(serverPort);

			while (true)
			{
				Socket connectionSocket = welcomeSocket.accept();
				TCPServer thread = new TCPServer(connectionSocket,middle);
				thread.start();
			}
		}
		catch (Exception e)
		{
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
		}
	}

	public TCPMiddleware(String name)
	{
		super(name);
	}
}
