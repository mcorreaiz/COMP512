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
	protected static HashMap RMServers = new HashMap();

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

	public int start() 
    throws RemoteException 
	{
		testAndReconnectRMS();
		return super.start();
	}

    public boolean commit(int transactionId) 
    throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.commit(transactionId);
	}

    public void abort(int transactionId) 
    throws RemoteException, InvalidTransactionException
	{
		testAndReconnectRMS();
		super.abort(transactionId);
	}

    public boolean shutdown() 
    throws RemoteException
	{
		testAndReconnectRMS();
		return super.shutdown();
	}

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.addFlight(id, flightNum, flightSeats, flightPrice);
	}

    public boolean addCars(int id, String location, int numCars, int price)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.addCars(id, location, numCars, price);
	}

    public boolean addRooms(int id, String location, int numRooms, int price)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.addRooms(id, location, numRooms, price);
	}

    public int newCustomer(int id)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.newCustomer(id);
	}

    public boolean newCustomer(int id, int cid)
    throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.newCustomer(id, cid);
	}

    public boolean deleteFlight(int id, int flightNum)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.deleteFlight(id, flightNum);
	}

    public boolean deleteCars(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.deleteCars(id, location);
	}

    public boolean deleteRooms(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.deleteRooms(id, location);
	}

    public boolean deleteCustomer(int id, int customerID)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.deleteCustomer(id, customerID);
	}

    public int queryFlight(int id, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryFlight(id, flightNumber);
	}

    public int queryCars(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryCars(id, location);
	}

    public int queryRooms(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryRooms(id, location);
	}

    public String queryCustomerInfo(int id, int customerID)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryCustomerInfo(id, customerID);
	}

    public int queryFlightPrice(int id, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryFlightPrice(id, flightNumber);
	}

    public int queryCarsPrice(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryCarsPrice(id, location);
	}

    public int queryRoomsPrice(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.queryRoomsPrice(id, location);
	}

    public boolean reserveFlight(int id, int customerID, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.reserveFlight(id, customerID, flightNumber);
	}

    public boolean reserveCar(int id, int customerID, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.reserveCar(id, customerID, location);
	}

    public boolean reserveRoom(int id, int customerID, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.reserveRoom(id, customerID, location);
	}

	public boolean unreserveFlight(int xid, int customerID, int flightNum) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		testAndReconnectRMS();
		return super.unreserveFlight(xid, customerID, flightNum);
	}

	public boolean unreserveCar(int xid, int customerID, String location) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		testAndReconnectRMS();
		return super.unreserveCar(xid, customerID, location);
	}

	public boolean unreserveRoom(int xid, int customerID, String location) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		testAndReconnectRMS();
		return super.unreserveRoom(xid, customerID, location);
	}

    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException
	{
		testAndReconnectRMS();
		return super.bundle(id, customerID, flightNumbers, location, car, room);
	}

    public String getName()
    throws RemoteException
	{
		testAndReconnectRMS();
		return super.getName();
	}

    public boolean prepare(int xid)
    throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		testAndReconnectRMS();
		return super.prepare(xid);
	}

    public void resetCrashes()
	throws RemoteException
	{
		testAndReconnectRMS();
		super.resetCrashes();
	}

    public void crashMiddleware(int mode)
	throws RemoteException
	{
		testAndReconnectRMS();
		super.crashMiddleware(mode);
	}

    public void crashResourceManager(String name /* RM Name */, int mode) 
    throws RemoteException
	{
		testAndReconnectRMS();
		super.crashResourceManager(name, mode);
	}
}
