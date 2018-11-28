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
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager implements IResourceManager
{

	protected static int CRASHMODE = 0;
	private static int CONNECTION_TIMEOUT = 90000;

	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	private LockManager lockManager = new LockManager();
	protected static HashMap s_resourceManagers;

	private static HashMap<String, Integer> LocationMapCar = new HashMap<String, Integer>();
	private static HashMap<String, Integer> LocationMapRoom = new HashMap<String, Integer>();

	private HashMap<Integer, RMHashMap> beforeImageLog = new HashMap<Integer, RMHashMap>();
	private HashSet<Integer> startedTransactions = new HashSet<Integer>();
	private HashSet<Integer> abortedTransactions = new HashSet<Integer>();

	//manage transaction timeout 
	private ConcurrentHashMap<Integer, Thread> timeTable = new ConcurrentHashMap<Integer, Thread>();
	protected static HashMap<Integer, Transaction> persistLog = new HashMap<Integer, Transaction>();

	private String masterRecordFile = "/tmp/masterRecord.ser";
	private String dbAFile = "/tmp/dbA.ser";
	private String dbBFile = "/tmp/dbB.ser";
	private String dbCommittedFile = dbAFile;
	private String logFile = "/tmp/log.ser";

	/**
     * The voting request method for 2PC 
     * @return boolean
     * yes for ready to commit, no for abort
     */
    public boolean prepare(int xid)
	throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		if (!persistLog.containsKey(xid)) return false;

		if (CRASHMODE == 1){
			System.exit(1);
		}

		// Check if transaction was aborted
		if (abortedTransactions.contains(xid)) return false;
		
		// Read last comitted copy of db
		RMHashMap committedData = null;
		try {
			FileInputStream fileIn = new FileInputStream(dbCommittedFile);
			if (fileIn.available() != 0)
			{
				ObjectInputStream in = new ObjectInputStream(fileIn);
				committedData = (RMHashMap) in.readObject();
				in.close();
				fileIn.close();
				System.out.println("Read last committed data:\n" + committedData + "\n");

			}
			else
			{
				committedData = (RMHashMap)this.m_data.clone();
				System.out.println("create first version of committed data\n" + committedData + "\n");
			}
			
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}

		// Write only the corresponding data
		if (hasImage(xid)) {
			RMHashMap image = readImage(xid);
			for (String key : image.keySet()) {
				RMItem item = readData(xid, key);
				committedData.put(key, item);
			}
		}
		else if (persistLog.containsKey(xid)) {
			// Committing from a recently read log
			RMHashMap image = persistLog.get(xid).data;
			for (String key : image.keySet()) {
				committedData.put(key, image.get(key));
				writeData(xid, key, image.get(key));
			}

		}

		// Create and write dbFile in-progress
		try {
			FileOutputStream fileOut = new FileOutputStream(getInProgressFilename());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(committedData);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
		System.out.println("Write updated ready-to-commit data:\n" + committedData + "\n");

		writeLog(xid, "yes");

		if (CRASHMODE == 2){
			System.exit(1);
		}

		if (CRASHMODE == 3){
			new Thread(new Runnable() {
				@Override
				public void run() {
					try{
						Thread.sleep(2000);
						System.exit(1);
					}
					catch(InterruptedException e){
						System.exit(1);
					}
				}
			}).start();
		}

		killTimer(xid);
		removeTimer(xid);
		return true;
	}

    /**
     * disable crashes
     * reset crash mode to 0 (no crash)
     */
	public void resetCrashes() throws RemoteException
	{
		Trace.info("RM::resetCrashes");
		CRASHMODE = 0;
	}


    /**
     * enable crashes for middleware
     * set new crash mode
     */
	public void crashMiddleware(int mode) throws RemoteException
	{

	}

    /**
     * enable crashes for resource managers
     * set new crash mode
     */
    public void crashResourceManager(String name /* RM Name */, int mode) 
	throws RemoteException
	{
		Trace.info("Middleware::crashResourceManager" + mode);
		if (name.equals(m_name)){
			CRASHMODE = mode;
		}
	}

	public ResourceManager(String p_name) {
		m_name = p_name;
		masterRecordFile = m_name + masterRecordFile;
		dbCommittedFile = m_name + dbCommittedFile;
		dbAFile = m_name + dbAFile;
		dbBFile = m_name + dbBFile;
		logFile = m_name + logFile;

		// Recovery
		checkOrCreateFiles();
		restoreDB();
		restartProtocal();
	}

	private void checkOrCreateFiles() {
		try {
			File tmpFile = new File(masterRecordFile);
			// if master file exists, there must be a committed version
			if (!tmpFile.exists()) 
			{               
				tmpFile.getParentFile().mkdirs();
				tmpFile.createNewFile();
				Trace.info("new persistent master record created at " + tmpFile.getParentFile().getAbsolutePath());

				File tmpFile2 = new File(dbAFile);
				if (!tmpFile2.exists()) 
				{               
					tmpFile2.getParentFile().mkdirs();
					tmpFile2.createNewFile();
				}

				File tmpFile3 = new File(dbBFile);
				if (!tmpFile3.exists()) 
				{               
					tmpFile3.getParentFile().mkdirs();
					tmpFile3.createNewFile();
				}
				
				//create the persistant logs
				File tmpFile4 = new File(logFile);
				if (!tmpFile4.exists()) 
				{               
					tmpFile4.getParentFile().mkdirs();
					tmpFile4.createNewFile();
				}

			}
			else {
				Trace.info("persistent master record exists at " + tmpFile.getParentFile().getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void restoreDB() {
		HashMap hm = null;

		try {
			// Restore master record
			FileInputStream fileIn = new FileInputStream(masterRecordFile);
			if (fileIn.available() > 0)
			{
				ObjectInputStream in = new ObjectInputStream(fileIn);
				hm = (HashMap<String,String>) in.readObject();
				in.close();
				fileIn.close();

				int tid = Integer.parseInt(hm.get("tid").toString()); // Do sth with this guy
				Trace.info("TID is " + tid);
				dbCommittedFile = hm.get("filename").toString();
				Trace.info("reading committed db file at " + dbCommittedFile);

				// Restore db file
				fileIn = new FileInputStream(dbCommittedFile);
				if (fileIn.available() > 0)
				{
					in = new ObjectInputStream(fileIn);
					m_data = (RMHashMap) in.readObject(); // Restore
					Trace.info("Data recovered:\n" + m_data);
					in.close();
					fileIn.close();
				}
			}			
		} 
		catch (IOException i) {
		i.printStackTrace();
		} 
		catch (ClassNotFoundException c) {
		c.printStackTrace();
		}
	}

	private void updateMasterRecord(int xid) {
		HashMap hm = new HashMap<String, String>();
		hm.put("tid", Integer.toString(xid));
		hm.put("filename", getInProgressFilename());

		try {
			FileOutputStream fileOut = new FileOutputStream(masterRecordFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(hm);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
		System.out.println("Updated master record:\n" + hm);
	}
	
	private void persistLogFile()
	{
		try 
		{
			FileOutputStream fileOut = new FileOutputStream(logFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(persistLog);
			out.close();
			fileOut.close();
		} 
		catch (IOException i) 
		{
			i.printStackTrace();
		}
		//Trace.info("Updated Log Files:\n");
	}

	private void writeLog(int xid, String log) throws TransactionAbortedException, InvalidTransactionException
	{
		synchronized(persistLog) {
			//create new log
			if (persistLog.get(xid) == null) {
				Transaction txn = new Transaction(xid);
				persistLog.put(xid,txn);
			}
			//updating existing log
			else {
				RMHashMap afterImage = new RMHashMap();
				if (hasImage(xid)) {
					RMHashMap image = readImage(xid);
					for (String key : image.keySet()) {
						RMItem item = readData(xid, key);
						afterImage.put(key, item);
					}
				}

				Transaction txn = (Transaction)persistLog.get(xid);
				txn.addLog(log);
				txn.setData(afterImage);
				persistLog.put(xid,txn);
				Trace.info("Transaction " + xid + " has been logged: " + log + "\nData:\n" + afterImage);
			}
		}
		persistLogFile();
	}
	
	private String readLog(int xid){
		Transaction txn = (Transaction)persistLog.get(xid);
		return txn.latestLog();
	}

	private void deleteLog(int xid) {
		synchronized (persistLog) {
			persistLog.remove(xid);
		}
	}

	private void restartProtocal() {
		// Keep in mind: DB is already restored!

		if (CRASHMODE == 5){
			System.exit(1);
		}

		// Load the Log
		try {
			FileInputStream fileIn = new FileInputStream(logFile);
			if (fileIn.available() > 0)
			{
				ObjectInputStream in = new ObjectInputStream(fileIn);
				persistLog = (HashMap<Integer, Transaction>) in.readObject();
				in.close();
				fileIn.close();
				Trace.info("Loaded log Files");
			}
			for (int key : persistLog.keySet()) {
				System.out.println("TID=" + key + ": " + persistLog.get(key));
			}			
		} 
		catch (IOException i) 
		{
			i.printStackTrace();
		} 
		catch (ClassNotFoundException c) 
		{
			System.out.println("Class not found");
			c.printStackTrace();
		}

		// Operate accordingly for each log
		try {
			if (persistLog.size()>0){
				for ( Integer key : persistLog.keySet() ) {
					Transaction txn = persistLog.remove(key);
					String logMsg = txn.latestLog();
					Trace.info("Log Message [tid=" + txn.xid + "]: " + logMsg);
					if (logMsg.equals("Empty")){
						abort(txn.xid);
					}
					else if (logMsg.equals("yes")){
						startedTransactions.add(txn.xid);
					}
					else if (logMsg.equals("abort")){
						abort(txn.xid);
					}
					else if (logMsg.equals("commit")){
						commit(txn.xid);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeImage(int xid, String key, RMItem value)
	{
		synchronized(beforeImageLog) {
			RMHashMap hm = beforeImageLog.get(xid);
			if (value == null) {
				hm.put(key, value);
			} else {
				hm.put(key, (RMItem)value.clone());
			}
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

	private String getInProgressFilename() {
		if (dbCommittedFile.equals(dbAFile)) {
			return dbBFile;
		} else {
			return dbAFile;
		}
	}

	public int start() throws RemoteException
	{
		return 512;
	}

	private void startTx(int xid) throws TransactionAbortedException, InvalidTransactionException
	{
		// Start Tx timer and beforeImage
		addImage(xid);
		startedTransactions.add(xid);
		startTimer(xid);

		//add a new transaction Log
		writeLog(xid, "");
	}

	public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		// Delete transaction from log
		Trace.info("RM::commit(" + transactionId + ") called");

		if (!persistLog.containsKey(transactionId)) return false;

		if (hasImage(transactionId)) {
			writeLog(transactionId, "commit");

			if (CRASHMODE == 4){
				System.exit(1);
			}

			//update master record to point to the current committed version
			updateMasterRecord(transactionId);

			removeImage(transactionId);
			killTimer(transactionId);
			removeTimer(transactionId);
		}
		deleteLog(transactionId);
		return lockManager.UnlockAll(transactionId);
	}

	public void abort(int transactionId) throws RemoteException, InvalidTransactionException
	{
		Trace.info("RM::abort(" + transactionId + ") called");

		if (abortedTransactions.contains(transactionId)) return;
		try{
			writeLog(transactionId, "abort");
		}
		catch (TransactionAbortedException e){		}

		if (CRASHMODE == 4){
			System.exit(1);
		}

		abortedTransactions.add(transactionId);

		// Undo all ops.
		if (hasImage(transactionId)) {
			RMHashMap image = readImage(transactionId);
			synchronized(m_data) {
				for (String key : image.keySet()) {
					m_data.put(key, image.get(key));
				}
			}
		}
		// Delete transaction from log
		removeImage(transactionId);
		killTimer(transactionId);
		removeTimer(transactionId);
		lockManager.UnlockAll(transactionId);

		deleteLog(transactionId);		
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
				startTx(xid);
			}
		} else {
			resetTimer(xid);
		}
		try {
			lockManager.Lock(xid, key, lock);
		}
		catch (DeadlockException e){
			try {
				abort(xid);
			} catch (RemoteException r) {
				r.printStackTrace();
			}
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
			RMItem item = readData(xid, key);
			System.out.println("Write log");
			writeImage(xid, key, item);
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
			writeImage(xid, key, item);
			writeImage(xid, customer.getKey(), customer);

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

	// keeping track of middleware timeout 
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
}
