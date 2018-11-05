package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class ClientSimulator
{
	private  String s_serverHost = "localhost";
	private  int s_serverPort = 1099;
	private  String s_serverName = "Middleware";

	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private  String s_rmiPrefix = "group9";

	private  int numClients = 10;
    private  int queriesPerSecond = 5;
	private  AutomatedClient clients[] = new AutomatedClient[numClients];

	private int averageSum;
    
    public  void main(String args[])
	{	
		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			numClients = Integer.parseInt(args[1]);
		}
		if (args.length > 2)
		{
			queriesPerSecond = Integer.parseInt(args[2]);
		}
		if (args.length > 3)
		{
			System.err.println((char)27 + "[31;1mAutomatedClient exception: " + (char)27 + "[0mUsage: java client.AutomatedClient [server_hostname]");
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
				AutomatedClient ac = new AutomatedClient(this, client.m_resourceManager, queriesPerSecond);
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
	
	public void threadDone(long average) {
		averageSum += average;
	}
}
