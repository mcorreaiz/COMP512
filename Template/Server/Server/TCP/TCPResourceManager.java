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
	private static int s_serverPort = 3000;

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
		}

		// Create the RMI server entry
		try {
			// Create a new Server object
			TCPResourceManager server = new TCPResourceManager(s_serverName);

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

	public TCPResourceManager(String name)
	{
		super(name);
	}
}
