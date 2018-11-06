package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class ClientSimulator
{
	protected static String s_serverHost = "localhost";
	protected static int s_serverPort = 1099;
	protected static String s_serverName = "Middleware";

	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	protected static String s_rmiPrefix = "group9";

	protected static int numClients = 0;
    protected static int numQueries = 0;
    protected static int queriesPerSecond = 0;
	protected static AutomatedClient clients[] = new AutomatedClient[16];

	//protected static int averageSum;
    
    public static void main(String args[])
	{	
		if (args.length == 0)
		{
			System.out.println("./run_automate serverHost #client #QueryPerClient #queriesPerSecond");
		}

		if (args.length > 0)
		{
			s_serverHost = args[0];
		}
		if (args.length > 1)
		{
			numClients = Integer.parseInt(args[1]);
			clients = new AutomatedClient[numClients];
		}
		if (args.length > 2)
		{
			numQueries = Integer.parseInt(args[2]);
		}
		if (args.length > 3)
		{
			queriesPerSecond = Integer.parseInt(args[3]);
		}
		if (args.length > 4)
		{
			System.err.println((char)27 + "[31;1mAutomatedClient exception: " + (char)27 + "[0mUsage: java client.AutomatedClient [server_hostname]");
			System.exit(1);
		}

		System.out.println("Ready to run " + numClients + " clients for " + queriesPerSecond + " queries per second");
		System.out.println( "and total number of queries are " + numQueries);

		// Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("performance.txt"));
			// writer.write("Number of Clients,Total Tx,Avg. Response Time\n");
			writer.write("Number of Clients,Tx per Second,Avg. Response Time\n");
			writer.close();
		}
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		int clientNumbers[] = {2, 4, 8, 16};
		int qps[] = {1024, 2048, 4096, 8192};
		// int queries[] = {50, 100, 200, 400, 600, 800};

		if (numClients == 0) {
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 4; j++) {
					//runTests(1, queries[j]*10, queries[j]*10);
					runTests(clientNumbers[i], qps[j]*10, qps[j]);
				}
			}
		} else {
			runTests();
		}
	}

	public static void runTests(int _numClients, int _numQueries, int _queriesPerSecond) {
		numClients = _numClients;
		numQueries = _numQueries;
		queriesPerSecond = _queriesPerSecond;
		runTests();
	}

	public static void runTests() {
		for (int i=0; i < numClients; i++) { // Create clients
            // Get a reference to the RMIRegister
            try {
                RMIClient client = new RMIClient();
                client.connectServer(s_serverHost, s_serverPort, s_serverName);
				AutomatedClient ac = new AutomatedClient(client.m_resourceManager, numQueries / numClients, queriesPerSecond / (double)numClients, numClients);
				clients[i] = ac;
            } 
            catch (Exception e) {    
                System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
			}
        }
		for (int i=0; i < numClients; i++) { // Run 'em
            // Get a reference to the RMIRegister
            try {
				clients[i].start();
            } 
            catch (Exception e) {    
                System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
			}
        }
        try {
			while (clients[numClients-1].isAlive()) {}
			Thread.sleep(2000);
			writeFile(Long.toString(getAverage()));
			// for (int i=0; i < numClients; i++) { // Get measurements
			// 	// Get a reference to the RMIRegister
			// 		AutomatedClient ac = clients[i];
			// 		ac.print("My average performance was: " + Long.toString(ac.timePerQuery) + "ms");
			// }
		}
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static long getAverage() {
		long sum = 0;
		for (int i=0; i < numClients; i++) { // Get measurements
			AutomatedClient ac = clients[i];
			ac.print("My average performance was: " + Long.toString(ac.timePerQuery) + "ms");
			sum += ac.timePerQuery;
		}
		return sum / numClients;
	}

	public static void writeFile(String results) throws IOException {		
		BufferedWriter writer = new BufferedWriter(new FileWriter("performance.txt", true));
		writer.write(String.format("%d,%d,%s\n", numClients, queriesPerSecond, results));
		writer.close();
	}
		
	// public void threadDone(long average) {
	// 	averageSum += average;
	// }
}
