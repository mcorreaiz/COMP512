package Server.Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

/**
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 *
 * failure reporting is done using two pieces, exceptions and boolean
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 *
 * If there is a boolean return value and you're not sure how it
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IResourceManager extends Remote
{
    /**
     * Start a transaction.
     *
     * @return xid of a new transaction
     */
    public int start() 
    throws RemoteException;;

    /**
     * Commit a specific transaction.
     *
     * @return success
     */
    public boolean commit(int transactionId) 
    throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Abort a specific transaction.
     *
     * @return void
     */
    public void abort(int transactionId) 
    throws RemoteException, InvalidTransactionException;;

    /**
     * Gracefully shutdown all servers.
     *
     * @return void
     */
    public boolean shutdown() 
    throws RemoteException;;

    /**
     * Add seats to a flight.
     *
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Add car at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Add room at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
        throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Delete the flight.
     *
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */
    public boolean deleteFlight(int id, int flightNum)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Delete all cars at a location.
     *
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */
    public boolean deleteCars(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Delete all rooms at a location.
     *
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;



	// remove reservation for flight
	public boolean unreserveFlight(int xid, int customerID, int flightNum) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException;;


	// remove reservation for car
	public boolean unreserveCar(int xid, int customerID, String location) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException;;


	// remove reservation for room
	public boolean unreserveRoom(int xid, int customerID, String location) 
	throws RemoteException,TransactionAbortedException, InvalidTransactionException;;

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
	throws RemoteException, InvalidTransactionException, TransactionAbortedException;;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
    throws RemoteException;

    /**
     * The voting request method for 2PC 
     * @return boolean
     * yes for ready to commit, no for abort
     */
    public boolean prepare(int xid)
    throws RemoteException, TransactionAbortedException, InvalidTransactionException;

    /**
     * disable crashes
     * reset crash mode to 0 (no crash)
     */
    public void resetCrashes() throws RemoteException;


    /**
     * enable crashes for middleware
     * set new crash mode
     */
    public void crashMiddleware(int mode) throws RemoteException;


    /**
     * enable crashes for resource managers
     * set new crash mode
     */
    public void crashResourceManager(String name /* RM Name */, int mode) 
    throws RemoteException;





}
