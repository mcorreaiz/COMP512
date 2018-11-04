package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.util.concurrent.ConcurrentHashMap;

public class Middleware implements IResourceManager
{

	// Middleware just pass the arguments along
	protected String m_name = "";

	// Transaction Manager component
	protected Integer highestXid;
	protected static ArrayList<Integer> abortedT = new ArrayList<Integer>();
	protected static HashMap<Integer, String> transactionInfo;
	private static int CONNECTION_TIMEOUT = 60000;
	//manage transaction timeout 
	private ConcurrentHashMap<Integer, Thread> timeTable = new ConcurrentHashMap<Integer, Thread>();
	
	//resource managers 
	protected static HashMap s_resourceManagers;
	//client IDs
	protected static ArrayList<Integer> CIDs = new ArrayList<Integer>();
	protected static IResourceManager car_Manager = null;
	protected static IResourceManager flight_Manager = null;
	protected static IResourceManager room_Manager = null;



	// keeping track of timeout 
	public class TimeOutThread implements Runnable {
		private int xid = 0;

		public TimeOutThread(int xid) {
			this.xid = xid;
		}

		@Override
		public void run() {
			try 
			{
				Thread.sleep(CONNECTION_TIMEOUT);
			} 
			catch (InterruptedException e) 
			{
				//exit if interrupted
				Thread.currentThread().interrupt();
				return;
			}

			try 
			{
				Trace.info("Middleware::Transaction" + Integer.toString(xid) + " connection timeout");
				abort(this.xid);
			} 
			catch (InvalidTransactionException e) 
			{
				Trace.info("Middleware::Transaction" + Integer.toString(xid) + " is no longer valid.");
			} 
			catch (RemoteException e) 
			{
				Trace.info("Middleware::Transaction" + Integer.toString(xid) + " has remote exception");
			}
		}
	}

	// start a timer for a new transaction
	private void startTimer(int xid) {
		Thread now = new Thread(new TimeOutThread(xid));
		now.start();
		timeTable.put(xid, now);
		Trace.info("timer has been reset for transaction " + xid);
	}

	// reset a Timer when new activity arrives 
	private synchronized void resetTimer(int xid) throws InvalidTransactionException, TransactionAbortedException {
		// do nothing if it is not in the time table
		if (timeTable.get(xid) != null) 
		{
			killTimer(xid);
			startTimer(xid);
		} 
	}

	// eliminate outdated timer thread
	public void killTimer(int id) {
		Thread p = timeTable.get(id);
		if (p != null) {
			p.interrupt();
		}
	}

	//remove a timer when transaction is either commited or aborted
	private void removeTimer(int xid) {
		timeTable.remove(xid);
	}

	public Middleware(String p_name)
	{
		m_name = p_name;
	}

	public void initialize()
	{
		car_Manager = (IResourceManager)s_resourceManagers.get("Cars");
		flight_Manager = (IResourceManager)s_resourceManagers.get("Flights");
		room_Manager = (IResourceManager)s_resourceManagers.get("Rooms");
		transactionInfo = new HashMap<Integer, String>();
		highestXid = 0;
		Trace.info("All Managers connected and ready to roll");
	}


	public int start() throws RemoteException
	{
		//initialize a transaction for all RMs
		int xid = incrementXid();
		writeTransaction(xid,"");
		startTimer(xid);
		return xid;
	}

	public boolean commit(int transactionId) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::commit(" + transactionId + ") called");
		checkExistence(transactionId);
		String existing = readTransaction(transactionId);

		if (existing.indexOf("car") >= 0)
		{
			Trace.info("Middleware asks nicely that car Manager should commit(" + transactionId + ")");
			car_Manager.commit(transactionId);
		}
		if (existing.indexOf("flight") >= 0)
		{
			Trace.info("Middleware asks nicely that flight Manager should commit(" + transactionId + ")");			
			flight_Manager.commit(transactionId);
		}
		if (existing.indexOf("room") >= 0)
		{
			Trace.info("Middleware asks nicely that room Manager should commit(" + transactionId + ")");
			room_Manager.commit(transactionId);
		}
		removeTransaction(transactionId);
		killTimer(transactionId);
		removeTimer(transactionId);
		return true;
	}

	public void abort(int transactionId) throws RemoteException,InvalidTransactionException
	{
		Trace.info("Middleware::abort(" + transactionId + ") called");
		try{
			checkExistence(transactionId);
		}
		catch (TransactionAbortedException e)
		{
			Trace.info(transactionId + " already aborted");
		}

		String existing = readTransaction(transactionId);
		System.out.println(existing);
		if (existing.indexOf("car") >= 0)
		{
			Trace.info("Middleware demands that car Manager must abort(" + transactionId + ")");			
			car_Manager.abort(transactionId);
		}
		if (existing.indexOf("flight") >= 0)
		{
			Trace.info("Middleware demands that flight Manager must abort(" + transactionId + ")");						
			flight_Manager.abort(transactionId);
		}
		if (existing.indexOf("room") >= 0)
		{
			Trace.info("Middleware demands that room Manager must abort(" + transactionId + ")");						
			room_Manager.abort(transactionId);
		}
		removeTransaction(transactionId);
		abortedT.add(transactionId);
		killTimer(transactionId);
		removeTimer(transactionId);
	}

	public boolean shutdown() throws RemoteException
	{
		Trace.info("Middleware::shutdown() called");
		boolean success = true;

		//abort all active transaction first 
		Iterator<Integer> xids;
		synchronized(transactionInfo){
			xids = transactionInfo.keySet().iterator();
		}
		while(xids.hasNext()){
			try{
				abort(xids.next());
			}
			catch(InvalidTransactionException e)
			{
				Trace.info("xid doesn't exist");
			}
		}

		Trace.info("Middleware asks car Manager to gracefully shutdown()");			
		success = success && (car_Manager.shutdown());
		Trace.info("Middleware then asks flight Manager to gracefully shutdown()");		
		success = success && (flight_Manager.shutdown());
		Trace.info("Lastly Middleware asks room Manager to gracefully shutdown()");	
		success = success && (room_Manager.shutdown());	

		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					System.out.println("Shutdown in 2 seconds");
					Thread.sleep(2000);
					System.exit(0);
				}
				catch(InterruptedException e){
					System.exit(0);
				}
			}   
		}).start();

		return success;
	}


	private int incrementXid()
	{
		synchronized(highestXid){
			highestXid++;
			return highestXid;
		}
	}

	private void writeTransaction(int xid, String value)
	{
		synchronized(transactionInfo) {
			transactionInfo.put(xid, value);
		}
	}

	private String readTransaction(int xid)
	{
		synchronized(transactionInfo) {
			return transactionInfo.get(xid);
		}
	}

	protected void removeTransaction(int xid)
	{
		synchronized(transactionInfo) {
			transactionInfo.remove(xid);
		}
	}

	// this transaction is associated with car_manager
	private void associateManager(int xid, String manager) throws RemoteException
	{
		String existing = readTransaction(xid);
		// if the manager is not already associated, add it to the association
		if (existing.indexOf(manager) == -1)
		{
			writeTransaction(xid, existing+manager);
		}
	}

	//check if a transaction is started or not
	private void checkExistence(int xid) throws TransactionAbortedException,InvalidTransactionException
	{
		synchronized(transactionInfo)
		{
			if (!transactionInfo.containsKey(xid))
			{
				if (abortedT.contains(xid))
				{
					throw new TransactionAbortedException("this transaction has been aborted");
				}
				else
				{
					throw new InvalidTransactionException("xid doesn't exist");
				}
			}
			else
			{
				resetTimer(xid);
			}
		}

	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		boolean success = true;

		checkExistence(xid);
		associateManager(xid,"car");
		if (car_Manager.addCars(xid, location, count, price)) {
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
					success = false;
				}
		return success;
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		boolean success = true;

		checkExistence(xid);
		associateManager(xid,"flight");
		if (flight_Manager.addFlight(xid, flightNum, flightSeats, flightPrice)) {
					System.out.println("Flight added");
				} else {
					System.out.println("Flight could not be added");
					success = false;
				}
		return success;
	}


	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		checkExistence(xid);
		associateManager(xid, "room");

		if (room_Manager.addRooms(xid, location, count, price)) {
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
				}
		return true;
	}

	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
		String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
		String.valueOf(Math.round(Math.random() * 100 + 1)));

		CIDs.add(cid);
		// create new customers on other RMs as well
		car_Manager.newCustomer(xid, cid);
		room_Manager.newCustomer(xid, cid);
		flight_Manager.newCustomer(xid, cid);
		// add cid to customer list
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::newCustomer(" + xid + ", " + customerID + ") called");
		boolean success = true;

		checkExistence(xid);
		associateManager(xid, "car");
		associateManager(xid, "flight");
		associateManager(xid, "room");

		success = success && (car_Manager.newCustomer(xid, customerID));
		success = success && (room_Manager.newCustomer(xid, customerID));
		success = success && (flight_Manager.newCustomer(xid, customerID));
		
		if (success)
		{
			CIDs.add(customerID);
			System.out.println("Add customer ID: " + customerID);
		}
		else
		{
			System.out.println("Customer could not be added");
		}
		return success;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteFlight(" + xid + ", " + flightNum + ") called");
		checkExistence(xid);
		associateManager(xid, "flight");
		if (flight_Manager.deleteFlight(xid, flightNum)) {
					System.out.println("Flight Deleted");
				} else {
					System.out.println("Flight could not be deleted");
				}
		return true;
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteCars(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "car");
		if (car_Manager.deleteCars(xid, location)) {
					System.out.println("Cars Deleted");
				} else {
					System.out.println("Cars could not be deleted");
				}
		return true;
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteRooms(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "room");
		if (room_Manager.deleteRooms(xid, location)) {
					System.out.println("Rooms Deleted");
				} else {
					System.out.println("Rooms could not be deleted");
				}
		return true;
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") called");
		boolean success = true;

		if (CIDs.contains(customerID)){
			checkExistence(xid);

			associateManager(xid, "car");
			associateManager(xid, "flight");
			associateManager(xid, "room");

			success = success && (car_Manager.deleteCustomer(xid, customerID));
			success = success && (room_Manager.deleteCustomer(xid, customerID));
			success = success && (flight_Manager.deleteCustomer(xid, customerID));

			if (success) 
			{
				CIDs.remove(customerID);
				Trace.info("Customer Deleted");
			} 
			else 
			{
				Trace.info("Customer could not be deleted");
			}
			return success;
		}
		else
		{
			Trace.info("Customer doesn't exit");
			return false;
		}
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryFlight(" + xid + ", " + flightNum + ") called");
		checkExistence(xid);
		int seats = flight_Manager.queryFlight(xid, flightNum);
		return seats;
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCars(" + xid + ", " + location + ") called");
		checkExistence(xid);
		int numCars = car_Manager.queryCars(xid, location);
		return numCars;		
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryRooms(" + xid + ", " + location + ") called");
		checkExistence(xid);
		int numRoom = room_Manager.queryRooms(xid, location);
		return numRoom;
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCustomerInfo(" + xid + ", " + customerID + ") called");	
		checkExistence(xid);		
		String bill = "";
		if (CIDs.contains(customerID))
		{
			bill += car_Manager.queryCustomerInfo(xid, customerID);
			bill += flight_Manager.queryCustomerInfo(xid, customerID);
			bill += room_Manager.queryCustomerInfo(xid, customerID);
			if (bill.equals("")){
				bill = "No bills found for customer " + customerID + "\n";
			}
			else{
				bill = "Bill for customer " + customerID + "is: \n" + bill;
			}
		}
		else
		{
			bill = "Customer " + customerID + " doesn't exist";
		}
		return bill;
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryFlightPrice(" + xid + ", " + flightNum + ") called");
		checkExistence(xid);
		int price = flight_Manager.queryFlightPrice(xid, flightNum);
		return price;
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCarsPrice(" + xid + ", " + location + ") called");
		checkExistence(xid);
		int price = car_Manager.queryCarsPrice(xid, location);
		return price;
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryRoomsPrice(" + xid + ", " + location + ") called");
		checkExistence(xid);
		int price = room_Manager.queryRoomsPrice(xid, location);
		return price;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ") called");
		checkExistence(xid);
		associateManager(xid, "flight");
		
		return (flight_Manager.reserveFlight(xid, customerID, flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{	
		Trace.info("Middleware::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");

		checkExistence(xid);
		associateManager(xid, "car");

		return (car_Manager.reserveCar(xid, customerID, location));
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		
		checkExistence(xid);
		associateManager(xid, "room");

		return (room_Manager.reserveRoom(xid, customerID, location));
	}

	// Reserve bundle 
	// if any of them false, revert all the changes 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::bundle(" + xid + ", " + customerId + ", " + flightNumbers + "," + location + "," + car + "," + room + ") called");
		//first reserve flights 
		int numItemReserved = 0;
		String[] flightNums = new String[flightNumbers.size()]; 
		flightNums = (String[]) flightNumbers.toArray(flightNums); 

		checkExistence(xid);
		associateManager(xid, "flight");


		for (int i = 0; i < flightNums.length; i++)  
        { 
        	int flightNum = Integer.parseInt(flightNums[i]);
        	Trace.info("Middleware::reserveFlight(" + xid + ", " + customerId + ", " + flightNum + ") in bundle");

        	if (this.reserveFlight(xid, customerId, flightNum)) 
        	{
        		System.out.println("Flight Reserved");
        		numItemReserved++;
			} 
			else 
			{
				System.out.println("Flight could not be reserved");
				//need to abort 
				return false;
			}
        }


        //reserve optional rooms & cars
        if(car==true)
        {
        	associateManager(xid, "car");

        	Trace.info("Middleware::reserveCar(" + xid + ", " + customerId + ", " + location + ") in bundle");
			if(this.reserveCar(xid, customerId, location))
			{
				System.out.println("Car Reserved");
				numItemReserved++;
			} 
			else
			{
				System.out.println("Car could not be reserved");
				//need to abort
				return false;
			}
        }

        if(room==true)
        {
        	associateManager(xid, "room");

        	Trace.info("Middleware::reserveRoom(" + xid + ", " + customerId + ", " + location + ") in bundle");
        	if(this.reserveRoom(xid, customerId, location)) 
        	{
        		System.out.println("Room Reserved");
        		numItemReserved++;
			} 
			else 
			{
				System.out.println("Room could not be reserved");
				//need to abort
				return false;
			}
        }
		return true;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 