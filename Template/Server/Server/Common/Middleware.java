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

	protected static int CRASHMODE = 0;
	private static int REPLY_TIMEOUT = 20000;
	private static int CONNECTION_TIMEOUT = 120000;



	// Middleware just pass the arguments along
	protected String m_name = "Middleware";

	// Transaction Manager component
	protected Integer highestXid;
	protected static ArrayList<Integer> abortedT = new ArrayList<Integer>();
	protected static HashMap<Integer, String> transactionInfo;
	protected static HashMap<Integer, Transaction> persistLog;
	//manage transaction timeout 
	private ConcurrentHashMap<Integer, Thread> timeTable = new ConcurrentHashMap<Integer, Thread>();
	
	//resource managers 
	protected static HashMap s_resourceManagers;
	protected static HashMap RMServers;
	//client IDs
	protected static IResourceManager car_Manager = null;
	protected static IResourceManager flight_Manager = null;
	protected static IResourceManager room_Manager = null;

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
	throws RemoteException, TransactionAbortedException, InvalidTransactionException{

		if (xid > highestXid){
			//this should never be the case
			return false;
		}
		else{

			if (abortedT.contains(xid)){
				return false;
			}
			else{
				synchronized(persistLog){
					//if it is in the active list of logged transaction
					if (persistLog.get(xid) != null){
						Transaction txn = persistLog.get(xid);
						if(txn.latestLog().equals("abort")){
							return false;
						}
						else if (txn.latestLog().equals("commit")){
							return true;
						}
						else{
							return false;
						}
					}
					else{
						return true;
					}
				}
			}
		}
	}

    /**
     * disable crashes
     * reset crash mode to 0 (no crash)
     */
	public void resetCrashes() throws RemoteException{
		Trace.info("Middleware::resetCrashes");
		CRASHMODE = 0;
		car_Manager.resetCrashes();
		flight_Manager.resetCrashes();
		room_Manager.resetCrashes();
	}


    /**
     * enable crashes for middleware
     * set new crash mode
     */
	public void crashMiddleware(int mode) throws RemoteException{
		Trace.info("Middleware::crashMiddleware" + mode);
		CRASHMODE = mode;
	}

    public void crashResourceManager(String name /* RM Name */, int mode) throws RemoteException{
		if (name.equals("Cars")){
			car_Manager.crashResourceManager("Cars", mode);
		}
		else if(name.equals("Flights")){
			flight_Manager.crashResourceManager("Flights", mode);
		}
		else if(name.equals("Rooms")){
			room_Manager.crashResourceManager("Rooms", mode);
		}
	}
	

	public Middleware(String p_name)
	{
		m_name = p_name;
		CRASHMODE = 0;
	}

	public void initialize(){
		car_Manager = (IResourceManager)s_resourceManagers.get("Cars");
		flight_Manager = (IResourceManager)s_resourceManagers.get("Flights");
		room_Manager = (IResourceManager)s_resourceManagers.get("Rooms");

		transactionInfo = new HashMap<Integer, String>();
		persistLog = new HashMap<Integer, Transaction>();
		highestXid = 0;
		
		masterRecordFile = m_name + masterRecordFile;
		dbCommittedFile = m_name + dbCommittedFile;
		dbAFile = m_name + dbAFile;
		dbBFile = m_name + dbBFile;
		logFile = m_name + logFile;
		checkOrCreateFiles();
		if (CRASHMODE == 8){
			System.exit(1);
		}
		restoreMasterRecord();
		restartProtocal();
		Trace.info("All Managers connected and ready to roll");
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
			else{
				Trace.info("persistent master record exists at " + tmpFile.getParentFile().getAbsolutePath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void restoreMasterRecord() {
		HashMap hm = null;
		Coordination coor = null;

		//try to load existing data logs
		try{
			FileInputStream fileIn = new FileInputStream(logFile);
			if (fileIn.available() > 0)
				{
					ObjectInputStream in = new ObjectInputStream(fileIn);
					persistLog = (HashMap<Integer,Transaction>) in.readObject();
					in.close();
					fileIn.close();
					restartProtocal();
				}
			}
			catch (IOException i) {
			i.printStackTrace();
			} 
			catch (ClassNotFoundException c) {
			System.out.println("class not found");
			c.printStackTrace();
			}
		
		//try to load the existing data 
		try {
			FileInputStream fileIn = new FileInputStream(masterRecordFile);
			if (fileIn.available() > 0)
			{
				ObjectInputStream in = new ObjectInputStream(fileIn);
				hm = (HashMap<String,String>) in.readObject();
				in.close();
				fileIn.close();
				int tid = Integer.parseInt(hm.get("tid").toString());
				Trace.info("tid is " + tid);
				dbCommittedFile = hm.get("filename").toString();
				Trace.info("reading committed db file at " + dbCommittedFile);
				fileIn = new FileInputStream(dbCommittedFile);
				if (fileIn.available() > 0){
					in = new ObjectInputStream(fileIn);
					coor = (Coordination) in.readObject();
					Trace.info("Data recovered:\n" + coor);

					//restore the important information
					highestXid = Integer.valueOf(coor.highestXid.intValue());
					abortedT = (ArrayList<Integer>)coor.abortedT.clone();
					transactionInfo = (HashMap<Integer, String>)coor.transactionInfo.clone();
					in.close();
					fileIn.close();
					}
				}			
			} 
			catch (IOException i) 
			{
				i.printStackTrace();
			} 
			catch (ClassNotFoundException c) 
			{
         		System.out.println("class not found");
         		c.printStackTrace();
			}
	}

	private void updateMasterRecord(int xid) {
		HashMap hm = new HashMap<String, String>();
		hm.put("tid", Integer.toString(xid));
		hm.put("filename", getInProgressFilename());

		try 
		{
			FileOutputStream fileOut = new FileOutputStream(masterRecordFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(hm);
			out.close();
			fileOut.close();
		} 
		catch (IOException i) 
		{
			i.printStackTrace();
		}
		System.out.println("Updated master record:\n" + hm);
	}

	private void restartProtocal(){
		//try to load the existing data 
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
			} 
			catch (IOException i) 
			{
				i.printStackTrace();
			} 
			catch (ClassNotFoundException c) 
			{
         		System.out.println("class not found");
         		c.printStackTrace();
		}

		if (CRASHMODE == 8){
			System.exit(1);
		}
		//operate correspondingly for each log
		Integer highest = new Integer(0);
		if (persistLog.size()>0){
			for ( Integer key : persistLog.keySet() ) {
				if (key.intValue() > highest.intValue()){
					highest = new Integer(key.intValue());
				}
				Transaction txn = persistLog.remove(key);
				if (txn.latestLog().equals("Empty")){
					abortAll(txn.xid);
				}
				else if (txn.latestLog().equals("abort")){
					abortAll(txn.xid);
				}
				else if (txn.latestLog().equals("commit")){
					commitAll(txn.xid);
				}
			}
		}
		if (highest.intValue() > highestXid.intValue()){
			highestXid = new Integer(highest.intValue());
		}
	}

	private void abortAll(int xid){
		
		try{
			car_Manager.abort(xid);
		}catch(Exception e){}
		try{
			flight_Manager.abort(xid);
		}catch(Exception e){}
		try{
			room_Manager.abort(xid);
		}catch(Exception e){}
		abortedT.add(xid);
	}

	private void commitAll(int xid){
		try{
			car_Manager.commit(xid);
		}catch(Exception e){}
		try{
			flight_Manager.commit(xid);
		}catch(Exception e){}
		try{
			room_Manager.commit(xid);
		}catch(Exception e){}
		removeTransaction(xid);
	}

	private void persistLogFile()
	{
		synchronized(persistLog){
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

		}
	}

	private String getInProgressFilename() {
		if (dbCommittedFile.equals(dbAFile)) {
			return dbBFile;
		} else {
			return dbAFile;
		}
	}

	private void writeLog(int xid, String log)
	{
		synchronized(persistLog) {
			//create new log
			if (persistLog.get(xid) == null) {
				Transaction txn = new Transaction(xid);
				persistLog.put(xid,txn);
			}
			//updating existing log
			else {
				Transaction txn = (Transaction)persistLog.get(xid);
				txn.addLog(log);
				persistLog.put(xid,txn);
				Trace.info("Transaction " + xid + " has been logged: " + log);
			}
		}
		persistLogFile();
	}

	//remove the log when it is end of transaction
	private void removeLog(int xid){
		synchronized(persistLog) {
			//create new log
			if(persistLog.get(xid) != null){
				persistLog.remove(xid);
			}
		}
	}

	private String readLog(int xid){
		Transaction txn = (Transaction)persistLog.get(xid);
		return txn.latestLog();
	}


	public int start() throws RemoteException
	{
		//initialize a transaction for all RMs
		int xid = incrementXid();
		writeTransaction(xid,"");
		startTimer(xid);

		//add a new transaction Log
		writeLog(xid, "");
		return xid;
	}

	public boolean commit(int transactionId) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
		Trace.info("Middleware::commit(" + transactionId + ") called");
		checkExistence(transactionId);
		//start 2PC protocal
		writeLog(transactionId, "Start2PC");
		String existing = readTransaction(transactionId);
		boolean success = true;
		//send out vote requests and start timers
		if (CRASHMODE == 1){
			System.exit(1);	
		}
		success = voteRequest(transactionId);

		if (success)
		{
			//write decision
			if (CRASHMODE == 4){
				System.exit(1);
			}
			writeLog(transactionId, "commit");
			
			if (CRASHMODE == 5){
				System.exit(1);
			}
			commit4Real(transactionId);

			killTimer(transactionId);
			removeTimer(transactionId);

			//only create shadow copy when commit is completed
			Coordination committedData = new Coordination(highestXid,transactionInfo,abortedT);
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
			System.out.println("Write updated committed data:\n" + committedData);

			//update master record to point to the current committed version
			updateMasterRecord(transactionId);
			
			dbCommittedFile = getInProgressFilename();
			removeTransaction(transactionId);
			removeLog(transactionId);
			return success;
		}
		else
		{
			return success;
		}
	}


	private boolean voteRequest(int transactionId) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		String existing = readTransaction(transactionId);
		boolean success = true;

		if (existing.indexOf("car") >= 0)
		{
			Trace.info("Middleware asks car Manager to vote for commit(" + transactionId + ")");
			Thread car = new Thread(new ReplyThread(transactionId, "car"));
			car.start();
			//same as not send
			if (CRASHMODE == 2){
				System.exit(1);	
			}
			success = success && (car_Manager.prepare(transactionId));
			if (CRASHMODE == 3){
				System.exit(1);
			}
			car.interrupt();
			if (!success)
			{
				writeLog(transactionId, "abort");
				this.abort(transactionId);
				return false;
			}
		}
		if (existing.indexOf("flight") >= 0)
		{
			Trace.info("Middleware asks flight Manager to vote for commit(" + transactionId + ")");
			Thread flight = new Thread(new ReplyThread(transactionId, "flight"));
			flight.start();
			if (CRASHMODE == 2){
				System.exit(1);	
			}
			success = success && (flight_Manager.prepare(transactionId));
			if (CRASHMODE == 3){
				System.exit(1);
			}
			flight.interrupt();
			if (!success)
			{
				writeLog(transactionId, "abort");
				this.abort(transactionId);
				return false;
			}
		}
		if (existing.indexOf("room") >= 0)
		{
			Trace.info("Middleware asks room Manager to vote for commit(" + transactionId + ")");
			Thread room = new Thread(new ReplyThread(transactionId, "room"));
			room.start();
			if (CRASHMODE == 2){
				System.exit(1);	
			}
			success = success && (room_Manager.prepare(transactionId));
			if (CRASHMODE == 3){
				System.exit(1);
			}
			room.interrupt();
			if (!success)
			{
				writeLog(transactionId, "abort");
				this.abort(transactionId);
				return false;
			}
		}

		return success;
	}

	private void commit4Real(int transactionId) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		String existing = readTransaction(transactionId);

		if (existing.indexOf("car") >= 0)
		{
			Trace.info("Middleware asks nicely that car Manager should commit(" + transactionId + ")");
			car_Manager.commit(transactionId);
			if (CRASHMODE == 6){
				System.exit(1);
			}
		}
		if (existing.indexOf("flight") >= 0)
		{
			Trace.info("Middleware asks nicely that flight Manager should commit(" + transactionId + ")");			
			flight_Manager.commit(transactionId);
			if (CRASHMODE == 6){
				System.exit(1);
			}
		}
		if (existing.indexOf("room") >= 0)
		{
			Trace.info("Middleware asks nicely that room Manager should commit(" + transactionId + ")");
			room_Manager.commit(transactionId);
			if (CRASHMODE == 6){
				System.exit(1);
			}
		}

		if (CRASHMODE == 7){
			System.exit(1);
		}

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

		//first write the decision
		writeLog(transactionId, "abort");

		String existing = readTransaction(transactionId);
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
		
		//persist the middleware hashmaps
		//only create shadow copy when abort is completed
		Coordination committedData = new Coordination(highestXid,transactionInfo,abortedT);
		// Create and write Coordination
		try {
			FileOutputStream fileOut = new FileOutputStream(getInProgressFilename());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(committedData);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
		System.out.println("Write updated committed data:\n" + committedData);

		//update master record to point to the current committed version
		updateMasterRecord(transactionId);
		
		dbCommittedFile = getInProgressFilename();
		removeLog(transactionId);
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
			if(transactionInfo.containsKey(xid)){
				transactionInfo.remove(xid);
			}
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
		try
		{
			if (car_Manager.addCars(xid, location, count, price)) {
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
					success = false;
				}
		}
		//caught a deadlock
		catch (TransactionAbortedException e)
		{
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
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
		try
		{
			if (flight_Manager.addFlight(xid, flightNum, flightSeats, flightPrice)) {
					System.out.println("Flight added");
				} else {
					System.out.println("Flight could not be added");
					success = false;
				}
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
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
		boolean success = true;
		try
		{
			if (room_Manager.addRooms(xid, location, count, price)) {
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
					success = false;
				}
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");

		}
		return success;
	}

	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
		String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
		String.valueOf(Math.round(Math.random() * 100 + 1)));

		try
		{
			// create new customers on other RMs as well
			car_Manager.newCustomer(xid, cid);
			room_Manager.newCustomer(xid, cid);
			flight_Manager.newCustomer(xid, cid);
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}

		Trace.info("Middleware::newCustomer(" + cid + ") returns ID=" + cid);
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

		try
		{
			success = success && (car_Manager.newCustomer(xid, customerID));
			success = success && (room_Manager.newCustomer(xid, customerID));
			success = success && (flight_Manager.newCustomer(xid, customerID));
		}
		catch (TransactionAbortedException e)
		{
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		
		if (success)
		{
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
		boolean success = true;

		try
		{
			if (flight_Manager.deleteFlight(xid, flightNum)) {
					System.out.println("Flight Deleted");
				} else {
					System.out.println("Flight could not be deleted");
					success = false;
				}

		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteCars(" + xid + ", " + location + ") called");
		boolean success = true; 
		checkExistence(xid);
		associateManager(xid, "car");

		try
		{
			if (car_Manager.deleteCars(xid, location)) {
					System.out.println("Cars Deleted");
				} else {
					System.out.println("Cars could not be deleted");
					success = false;
				}
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteRooms(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "room");
		boolean success = true;

		try
		{
			if (room_Manager.deleteRooms(xid, location)) {
					System.out.println("Rooms Deleted");
				} else {
					System.out.println("Rooms could not be deleted");
					success = false;
				}
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") called");
		boolean success = true;

		checkExistence(xid);

		associateManager(xid, "car");
		associateManager(xid, "flight");
		associateManager(xid, "room");

		try
		{

			success = success && (car_Manager.deleteCustomer(xid, customerID));
			success = success && (room_Manager.deleteCustomer(xid, customerID));
			success = success && (flight_Manager.deleteCustomer(xid, customerID));
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}

		if (success) 
		{
			Trace.info("Customer Deleted");
		} 
		else 
		{
			Trace.info("Customer could not be deleted");
		}
		return success;
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryFlight(" + xid + ", " + flightNum + ") called");
		checkExistence(xid);
		int seats = 0;
		associateManager(xid, "flight");
		try
		{
			seats = flight_Manager.queryFlight(xid, flightNum);
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return seats;
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCars(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "car");
		int numCars = 0;
		try
		{
			numCars = car_Manager.queryCars(xid, location);
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return numCars;		
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryRooms(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "room");
		int numRoom = 0;
		try
		{
			numRoom = room_Manager.queryRooms(xid, location);
		}
		catch(TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return numRoom;
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCustomerInfo(" + xid + ", " + customerID + ") called");	
		checkExistence(xid);
		associateManager(xid, "car");
		associateManager(xid, "flight");
		associateManager(xid, "room");		
		String bill = "";

		try
		{
			bill += car_Manager.queryCustomerInfo(xid, customerID);
			bill += flight_Manager.queryCustomerInfo(xid, customerID);
			bill += room_Manager.queryCustomerInfo(xid, customerID);
		}
		catch(TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		if (bill.equals("")){
			bill = "No bills found for customer " + customerID + "\n";
		}
		else{
			bill = "Bill for customer " + customerID + "is: \n" + bill;
		}
		return bill;
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryFlightPrice(" + xid + ", " + flightNum + ") called");
		checkExistence(xid);
		associateManager(xid, "flight");
		int price = 0;
		try{
			price = flight_Manager.queryFlightPrice(xid, flightNum);
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return price;
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryCarsPrice(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "car");
		int price = 0;
		try{
			price = car_Manager.queryCarsPrice(xid, location);
		}catch(TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return price;
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::queryRoomsPrice(" + xid + ", " + location + ") called");
		checkExistence(xid);
		associateManager(xid, "room");		
		int price = 0;
		try{
			price = room_Manager.queryRoomsPrice(xid, location);
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return price;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ") called");
		checkExistence(xid);
		associateManager(xid, "flight");
		boolean success = true;
		try{
			success = (flight_Manager.reserveFlight(xid, customerID, flightNum));
		}
		catch (TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{	
		Trace.info("Middleware::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");

		checkExistence(xid);
		associateManager(xid, "car");
		boolean success = true;
		try{
			success = (car_Manager.reserveCar(xid, customerID, location));
		}
		catch(TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("Middleware::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		
		checkExistence(xid);
		associateManager(xid, "room");
		boolean success = true;
		try{
			success = room_Manager.reserveRoom(xid, customerID, location);
		}
		catch(TransactionAbortedException e){
			abort(xid);
			throw new TransactionAbortedException("This transaction has been aborted");
		}
		return success;
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
				unbundle(xid,customerId,flightNumbers,location,car,room,numItemReserved);
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
				unbundle(xid,customerId,flightNumbers,location,car,room,numItemReserved);
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
				unbundle(xid,customerId,flightNumbers,location,car,room,numItemReserved);
				return false;
			}
        }
		return true;
	}


	private void unbundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room, int numItemReserved) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		System.out.println("Middleware::Undo bundle because one of more of reservation in bundle has failed");
		String[] flightNums = new String[flightNumbers.size()]; 
		flightNums = (String[]) flightNumbers.toArray(flightNums); 
		checkExistence(xid);

		if (numItemReserved > flightNums.length)
		{
			for (int i = 0; i < flightNums.length; i++)  
	        { 
	        	int flightNum = Integer.parseInt(flightNums[i]);
	        	Trace.info("Middleware::unreserveFlight(" + xid + ", " + customerId + ", " + flightNum + ") in bundle");

	        	if (this.unreserveFlight(xid, customerId, flightNum)) 
	        	{
	        		System.out.println("Flight unReserved");
				} 
				else 
				{
					System.out.println("unknown reason why Flight can't be unReserved");
				}
	        }

	        numItemReserved = numItemReserved - flightNums.length;

	        if (numItemReserved >= 1)
	        {
	        	Trace.info("Middleware::unreserveCar(" + xid + ", " + customerId + ", " + location + ") in bundle");
				if(this.unreserveCar(xid, customerId, location))
				{
					System.out.println("Car unReserved");
				} 
				else
				{
					System.out.println("unknown reason why Car can't be unReserved");
				}

	        }

	        numItemReserved--;

	        if (numItemReserved >= 1)
	        {
	        	Trace.info("Middleware::unreserveRoom(" + xid + ", " + customerId + ", " + location + ") in bundle");
	        	if(this.unreserveRoom(xid, customerId, location)) 
	        	{
	        		System.out.println("Room unReserved");
				} 
				else 
				{
					System.out.println("unknown reason why Room could not be reserved");
				}
	        }

		}
		else 
		{
			for (int i = 0; i < numItemReserved; i++)  
	        { 
	        	int flightNum = Integer.parseInt(flightNums[i]);
	        	Trace.info("Middleware::unreserveFlight(" + xid + ", " + customerId + ", " + flightNum + ") in bundle");

	        	if (this.unreserveFlight(xid, customerId, flightNum)) 
	        	{
	        		System.out.println("Flight unReserved");
				} 
				else 
				{
					System.out.println("unknown reason why Flight can't be unReserved");
				}
	        }
		}
	}


	public boolean unreserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return flight_Manager.unreserveFlight(xid, customerID, flightNum);
	}

	public boolean unreserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return car_Manager.unreserveCar(xid, customerID, location);
	}

	public boolean unreserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return room_Manager.unreserveRoom(xid, customerID, location);
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}

	// keeping track of timeout 
	public class ReplyThread implements Runnable {
		private String manager;
		private int xid;

		public ReplyThread(int xid, String manager) {
			this.manager = manager;
			this.xid = xid;
		}

		@Override
		public void run() {
			try 
			{
				Thread.sleep(REPLY_TIMEOUT);
			} 
			catch (InterruptedException e) 
			{
				//exit if interrupted
				Thread.currentThread().interrupt();
				return;
			}

			try 
			{
				Trace.info("Middleware::Resource Manager" + manager + " vote request timeout");
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


	// keeping track of client timeout 
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
 