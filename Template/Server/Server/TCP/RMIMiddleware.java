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
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleware extends Middleware
{
	private static String s_serverName = "Middleware";
	private static int s_serverPort = 1099;
	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group9";

	public static void main(String args[])
	{
		if (args.length > 3) {
			String[] names = {"Cars", "Flights", "Rooms", "Customers"};
			s_resourceManagers = new HashMap();
			try {
				System.out.println("try to connect to resource managers");
				for (int i = 0; i < 4; i++) {
					//connect to 4 RMs
					boolean first = true;
					while (true) {
						try {
							System.out.println("Trying to connect to " + names[i]);
							Registry registry = LocateRegistry.getRegistry(args[i], s_serverPort);
							s_resourceManagers.put(names[i], (IResourceManager)registry.lookup(s_rmiPrefix + names[i]));
							System.out.println("Connected to '" + names[i] + "' server [" + args[i] + ":" + s_serverPort + "/" + s_rmiPrefix + names[i] + "]");
							break;
						}
						catch (NotBoundException|RemoteException e) {
							if (first) {
								System.out.println("Waiting for '" + names[i] + "' server [" + args[i] + ":" + s_serverPort + "/" + s_rmiPrefix + names[i] + "]");
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
			server.start();
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
}
