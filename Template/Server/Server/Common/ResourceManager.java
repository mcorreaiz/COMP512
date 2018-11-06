// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	private LockManager lockManager = new LockManager();
	private HashMap<Integer, RMHashMap> beforeImageLog = new HashMap<Integer, RMHashMap>();
	private HashSet<Integer> startedTransactions = new HashSet<Integer>();
	protected static HashMap s_resourceManagers;
	private static HashMap<String, Integer> LocationMapCar = new HashMap<String, Integer>();
	private static HashMap<String, Integer> LocationMapRoom = new HashMap<String, Integer>();


	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}

	private void writeImage(int xid, String key, RMItem value)
	{
		synchronized(beforeImageLog) {
			RMHashMap hm = beforeImageLog.get(xid);
			hm.put(key, value);
			beforeImageLog.put(xid, hm);
		}
	}

	private RMHashMap readImage(int xid)
	{
		synchronized(beforeImageLog) {
			return beforeImageLog.get(xid);
		}
	}

	private boolean hasImage(int xid)
	{
		synchronized(beforeImageLog) {
			return beforeImageLog.containsKey(xid);
		}
	}

	private void removeImage(int xid)
	{
		synchronized(beforeImageLog) {
			beforeImageLog.remove(xid);
		}
	}
	
	public void addImage(int xid)
	{
		synchronized(beforeImageLog) {
			beforeImageLog.put(xid, new RMHashMap());
		}
	}

	public int start() throws RemoteException
	{
		return (512);
	}

	public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		// Delete transaction from log
		Trace.info("RM::commit(" + transactionId + ") called");
		removeImage(transactionId);
		return lockManager.UnlockAll(transactionId);
	}

	public void abort(int transactionId) throws RemoteException, InvalidTransactionException
	{
		// Undo all ops.
		Trace.info("RM::abort(" + transactionId + ") called");
		RMHashMap image = readImage(transactionId);
		synchronized(m_data) {
			for (String key : image.keySet()) {
				m_data.put(key, image.get(key));
			}
		}
		// Delete transaction from log
		removeImage(transactionId);
		lockManager.UnlockAll(transactionId);
	}

	public boolean shutdown() throws RemoteException
	{
		Trace.info("RM::shutdown() called");
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
		return true;
	}

	private void beforeFilter(int xid, String key, TransactionLockObject.LockType lock) throws TransactionAbortedException, InvalidTransactionException {
		if (!hasImage(xid)) {
			if (startedTransactions.contains(xid))  {
				// If there's no image for this Tx but it was once started, it means that it was committed/aborted
				throw new InvalidTransactionException();
			} else {
				// Else, new transaction.
				addImage(xid);
				startedTransactions.add(xid);
			}
		}
		try {
			lockManager.Lock(xid, key, lock);
		}
		catch (DeadlockException e){
			throw new TransactionAbortedException();
		}
	}

	// Reads a data item
	protected RMItem readData(int xid, String key) throws TransactionAbortedException, InvalidTransactionException
	{
		beforeFilter(xid, key, TransactionLockObject.LockType.LOCK_READ);
		synchronized(m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) throws TransactionAbortedException, InvalidTransactionException
	{
		beforeFilter(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
		synchronized(m_data) {
			if (!readImage(xid).containsKey(key)) {
				RMItem item = readData(xid, key);
				System.out.println("Write log");
				writeImage(xid, key, item);
			}
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key) throws TransactionAbortedException, InvalidTransactionException
	{	
		beforeFilter(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
		synchronized(m_data) {
			m_data.remove(key);
		}
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws TransactionAbortedException, InvalidTransactionException
	{

		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{
			writeImage(xid, key, customer);
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	// Reserve an item
	protected boolean unreserveItem(int xid, int customerID, String key, String location) throws TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::unreserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::unreserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::unreserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			customer.unreserve(key, location);
			writeData(xid, customer.getKey(), customer);

			// Increase the number of available items in the storage
			item.setCount(item.getCount() + 1);
			item.setReserved(item.getReserved() - 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::unreserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	protected boolean trackTotalCars(String key, int number)
	{
		// if there is no existing entry, create one in the hashmap
		if (LocationMapCar.get(key) == null)
		{
			LocationMapCar.put(key,number);
		}
		else
		{
			LocationMapCar.put(key,number);
		}
		return true;
	}

	protected boolean trackTotalRooms(String key, int number)
	{
		// if there is no existing entry, create one in the hashmap
		if (LocationMapRoom.get(key) == null)
		{
			LocationMapRoom.put(key,number);
		}
		else
		{
			LocationMapRoom.put(key,number);
		}
		return true;
	}


	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			trackTotalCars(location, count);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			trackTotalCars(location, count);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			trackTotalRooms(location,count);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			trackTotalRooms(location,count);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{

		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
		String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
		String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		}
		else
		{
			// Increase the reserved numbers of all reservable items which the customer reserved.
 			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet())
			{
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
				ReservableItem item  = (ReservableItem)readData(xid, reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ", " + ") called");
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Adds flight reservation to this customer
	public boolean unreserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::unreserveFlight(" + xid + ", " + customerID + ", " + flightNum + ", " + ") called");
		return unreserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean unreserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{		
		Trace.info("RM::unreserveCar(" + xid + ", " + customerID + ", " + location + ") called");
		return unreserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean unreserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::unreserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		return unreserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return false;
	}

	public int queryLocationPopularity(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		int availCars = queryNum(xid, Car.getKey(location));
		int totalCars = 0;
		if (LocationMapCar.get(location) != null)
		{
			totalCars = LocationMapCar.get(location);
		}
		int availRooms = queryNum(xid, Room.getKey(location));
		int totalRooms = 0;
		if (LocationMapRoom.get(location) != null)
		{
			totalRooms = LocationMapRoom.get(location);
		}
		int sum = totalCars - availCars + totalRooms - availRooms;
		return sum;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
