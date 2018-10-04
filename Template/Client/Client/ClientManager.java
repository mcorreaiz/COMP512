package Client;

import java.util.*;
import java.io.*;

public class ClientManager extends Client
{

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			s_serverName = args[1];
		}
		if (args.length > 2)
		{
			s_serverPort = Integer.parseInt(args[2]);
		}
		if (args.length > 3)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.ClientManager [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		try {
			ClientManager client = new ClientManager();
			client.start();
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public ClientManager()
	{
		super();
	}

	public void connectServer()
	{
		connectServer(s_serverHost, s_serverPort, s_serverName);
	}

	public void connectServer(String server, int port, String name)
	{
		try {
			boolean first = true;
			while (true) {
				try {
					TCPClient connTest = new TCPClient(server, port, name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "]");
					break;
				}
				catch (Exception e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
