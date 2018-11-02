package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class ClientSimulator
{
	private static String s_serverHost = "localhost";
	private static int s_serverPort = 1099;
	private static String s_serverName = "Server";

	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group9";

	private static int numClients = 10;
	private static AutomatedClient clients[] = new AutomatedClient[numClients];
    
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
			System.err.println((char)27 + "[31;1mAutomatedClient exception: " + (char)27 + "[0mUsage: java client.AutomatedClient [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

        for (int i=0; i < numClients; i++) {
            // Get a reference to the RMIRegister
            try {
                RMIClient client = new RMIClient();
                client.connectServer();
				AutomatedClient ac = new AutomatedClient(client.m_resourceManager);
				clients[i] = ac;
				ac.start();
            } 
            catch (Exception e) {    
                System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
			}
        }
    }
}
