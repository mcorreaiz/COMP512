// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleware extends Middleware
{
	private static String s_serverName = "Middleware";
	private static int s_serverPort = 1099;
	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group9";

	public static void main(String args[])
	{
		if (args.length > 2) {
			String[] names = {"Cars", "Flights", "Rooms"};
			s_resourceManagers = new HashMap();
			RMServers = new HashMap();
			try {
				System.out.println("try to connect to resource managers");
				for (int i = 0; i < 3; i++) {
					//connect to 4 RMs
					connectRM(args[i], names[i]);
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
			throw new IllegalArgumentException("Middleware must know about 3 other RM! missing RM");
		}

		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIMiddleware server = new RMIMiddleware(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(s_serverPort);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(s_serverPort);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});
			server.initialize();
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}

	public RMIMiddleware(String name)
	{
		super(name);
	}	

	private static void connectRM(String server, String name) {
		try {
			boolean first = true;
			while (true) {
				try {
					System.out.println("Trying to connect to " + name);
					Registry registry = LocateRegistry.getRegistry(server, s_serverPort);
					s_resourceManagers.put(name, (IResourceManager)registry.lookup(s_rmiPrefix + name));
					RMServers.put(name, server);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + s_serverPort + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + s_serverPort + "/" + s_rmiPrefix + name + "]");
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

	private void testAndReconnectRMS() throws RemoteException {
		String trying = "Cars";
		try {
			car_Manager.start(); // Just a ping() method
			trying = "Flights";
			flight_Manager.start(); // Just a ping() method
			trying = "Rooms";
			room_Manager.start(); // Just a ping() method
		}
		catch (ConnectException e) {
			Trace.info("Reconnecting to " + trying + " Manager");
			connectRM((String)RMServers.get(trying), trying);
			if (trying.equals("Cars")) {
				car_Manager = (IResourceManager)s_resourceManagers.get(trying);
			}
			if (trying.equals("Flights")) {
				flight_Manager = (IResourceManager)s_resourceManagers.get(trying);
			}
			if (trying.equals("Rooms")) {
				room_Manager = (IResourceManager)s_resourceManagers.get(trying);
			}

			testAndReconnectRMS(); // Recurse in case more than 1 RM is down
		}
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		testAndReconnectRMS();
		return super.newCustomer(xid, customerID);
	}
}
