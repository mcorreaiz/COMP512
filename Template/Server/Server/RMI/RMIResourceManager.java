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

public class RMIResourceManager extends ResourceManager
{
	private static String s_serverName = "Server";
	private static int s_serverPort = 1099;
	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	private static String s_rmiPrefix = "group9";

	private static HashMap s_resourceManagers;

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}
		System.out.println(args);
		if (args.length > 1) {
			// It's CustomerRM. Initialize other RM's stubs.
			String[] names = {"Cars", "Flights", "Rooms"};
			s_resourceManagers = new RMHashMap();
			try {
				for (int i = 0; i < 3; i++) {
					// Iterate over three RM's
					System.out.println("Trying to connect to " + names[i]);
					boolean first = true;
					while (true) {
						try {
							Registry registry = LocateRegistry.getRegistry(args[i+1], s_serverPort);
							s_resourceManagers.put(names[i], registry.lookup(s_rmiPrefix + names[i]));
							System.out.println("Connected to '" + names[i] + "' server [" + args[i+1] + ":" + s_serverPort + "/" + s_rmiPrefix + names[i] + "]");
							break;
						}
						catch (NotBoundException|RemoteException e) {
							if (first) {
								System.out.println("Waiting for '" + names[i] + "' server [" + args[i+1] + ":" + s_serverPort + "/" + s_rmiPrefix + names[i] + "]");
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
			try {
				System.out.println("Number of cars at this location: " + ((IResourceManager)s_resourceManagers.get("Cars")).queryCars(0, "0"));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIResourceManager server = new RMIResourceManager(s_serverName);

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

	public RMIResourceManager(String name)
	{
		super(name);
	}
}
