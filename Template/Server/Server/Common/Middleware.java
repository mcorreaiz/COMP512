package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.io.*;
import java.net.*;

public class Middleware implements IResourceManager
{

	// Middleware just pass the arguments along

	protected String m_name = "";
	protected static HashMap rm_connections;
	protected static int my_serverPort = 0;
	protected static String car_connection = null;
	protected static String flight_connection = null;
	protected static String room_connection = null;
	protected static String customer_connection = null;




	public Middleware(String p_name)
	{
		m_name = p_name;
	}

	public void start()
	{
		car_connection = (String)rm_connections.get("Cars");
		flight_connection = (String)rm_connections.get("Flights");
		room_connection = (String)rm_connections.get("Rooms");
		customer_connection = (String)rm_connections.get("Customers");
		System.out.println("All Managers connected and ready to roll");
	}


	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price)
	{
		System.out.println("Middleware add Cars");
		Trace.info("Middleware::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		boolean response = false;
		try
		{
			TCPConnection car_manager = new TCPConnection(car_connection,my_serverPort,"Cars");
			String cmd = "addCars,"+xid+","+location+","+count+","+price;
			response = toBoolean(car_manager.sendCommand(cmd));
		}
		catch (IOException e)
		{
			System.out.println("TCP connection error");
		}
		if (response)
		{
			System.out.println("Cars added");
			return true;
		}
		else
		{
			System.out.println("Cars could not be added");
			return false;
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
	{
		Trace.info("Middleware::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");

		// if (flight_Manager.addFlight(xid, flightNum, flightSeats, flightPrice)) {
		// 			System.out.println("Flight added");
		// 		} else {
		// 			System.out.println("Flight could not be added");
		// 		}

		return true;
	}


	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price)
	{
		Trace.info("Middleware::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		// if (room_Manager.addRooms(xid, location, count, price)) {
		// 			System.out.println("Rooms added");
		// 		} else {
		// 			System.out.println("Rooms could not be added");
		// 		}
		return true;
	}

	public int newCustomer(int xid)
	{
		Trace.info("Middleware::newCustomer(" + xid + ") called");

		// int customer = customer_Manager.newCustomer(xid);

		return 0;
	}

	public boolean newCustomer(int xid, int customerID)
	{
		Trace.info("Middleware::newCustomer(" + xid + ", " + customerID + ") called");
		// if (customer_Manager.newCustomer(xid, customerID)) {
		// 			System.out.println("Add customer ID: " + customerID);
		// 		} else {
		// 			System.out.println("Customer could not be added");
		// 		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum)
	{
		Trace.info("Middleware::deleteFlight(" + xid + ", " + flightNum + ") called");
		// if (flight_Manager.deleteFlight(xid, flightNum)) {
		// 			System.out.println("Flight Deleted");
		// 		} else {
		// 			System.out.println("Flight could not be deleted");
		// 		}
		return true;
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location)
	{
		Trace.info("Middleware::deleteCars(" + xid + ", " + location + ") called");
		// if (car_Manager.deleteCars(xid, location)) {
		// 			System.out.println("Cars Deleted");
		// 		} else {
		// 			System.out.println("Cars could not be deleted");
		// 		}
		return true;
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location)
	{
		Trace.info("Middleware::deleteRooms(" + xid + ", " + location + ") called");
		// if (room_Manager.deleteRooms(xid, location)) {
		// 			System.out.println("Rooms Deleted");
		// 		} else {
		// 			System.out.println("Rooms could not be deleted");
		// 		}
		return true;
	}

	public boolean deleteCustomer(int xid, int customerID)
	{
		Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") called");
		// if (customer_Manager.deleteCustomer(xid, customerID)) {
		// 			System.out.println("Customer Deleted");
		// 		} else {
		// 			System.out.println("Customer could not be deleted");
		// 		}
		return true;
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum)
	{
		Trace.info("Middleware::queryFlight(" + xid + ", " + flightNum + ") called");

		// int seats = flight_Manager.queryFlight(xid, flightNum);

		return 0;
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location)
	{
		Trace.info("Middleware::queryCars(" + xid + ", " + location + ") called");

		// int numCars = car_Manager.queryCars(xid, location);

		return 0;
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location)
	{
		Trace.info("Middleware::queryRooms(" + xid + ", " + location + ") called");

		// int numRoom = room_Manager.queryRooms(xid, location);

		return 0;
	}

	public String queryCustomerInfo(int xid, int customerID)
	{
		Trace.info("Middleware::queryCustomerInfo(" + xid + ", " + customerID + ") called");

		// String bill = customer_Manager.queryCustomerInfo(xid, customerID);

		return "you are broke!!!!";
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum)
	{
		Trace.info("Middleware::queryFlightPrice(" + xid + ", " + flightNum + ") called");

		// int price = flight_Manager.queryFlightPrice(xid, flightNum);

		return 0;
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location)
	{
		Trace.info("Middleware::queryCarsPrice(" + xid + ", " + location + ") called");

		// int price = car_Manager.queryCarsPrice(xid, location);

		return 0;
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location)
	{
		Trace.info("Middleware::queryRoomsPrice(" + xid + ", " + location + ") called");

		// int price = room_Manager.queryRoomsPrice(xid, location);

		return 0;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum)
	{
		Trace.info("Middleware::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ") called");
		// if (customer_Manager.reserveFlight(xid, customerID, flightNum)) {
		// 			System.out.println("Flight Reserved");
		// 		} else {
		// 			System.out.println("Flight could not be reserved");
		// 		}
		return true;
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location)
	{
		Trace.info("Middleware::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");
		// if (customer_Manager.reserveCar(xid, customerID, location)) {
		// 			System.out.println("Car Reserved");
		// 		} else {
		// 			System.out.println("Car could not be reserved");
		// 		}
		return true;
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location)
	{
		Trace.info("Middleware::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		// if (customer_Manager.reserveRoom(xid, customerID, location)) {
		// 			System.out.println("Room Reserved");
		// 		} else {
		// 			System.out.println("Room could not be reserved");
		// 		}
		return true;
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room)
	{
		Trace.info("Middleware::bundle(" + xid + ", " + customerId + ", " + flightNumbers + "," + location + "," + car + "," + room + ") called");
		// //first reserve flights
		// String[] flightNums = new String[flightNumbers.size()];
		// flightNums = (String[]) flightNumbers.toArray(flightNums);

		// for (int i = 0; i < flightNums.length; i++)
  //       {
  //       	int flightNum = Integer.parseInt(flightNums[i]);
  //       	Trace.info("Middleware::reserveFlight(" + xid + ", " + customerId + ", " + flightNum + ") in bundle");

  //       	if (customer_Manager.reserveFlight(xid, customerId, flightNum)) {
  //       		System.out.println("Flight Reserved");
		// 		}
		// 	else {
		// 		System.out.println("Flight could not be reserved");
		// 		}
  //       }

  //       //reserve optional rooms & cars
  //       if(car==true)
  //       {
  //       	Trace.info("Middleware::reserveCar(" + xid + ", " + customerId + ", " + location + ") in bundle");
		// 	if(customer_Manager.reserveCar(xid, customerId, location))
		// 	{
		// 		System.out.println("Car Reserved");
		// 	}
		// 	else
		// 	{
		// 		System.out.println("Car could not be reserved");
		// 	}
  //       }

  //       if(room==true)
  //       {
  //       	Trace.info("Middleware::reserveRoom(" + xid + ", " + customerId + ", " + location + ") in bundle");
  //       	if(customer_Manager.reserveRoom(xid, customerId, location))
  //       	{
  //       		System.out.println("Room Reserved");
		// 	}
		// 	else
		// 	{
		// 		System.out.println("Room could not be reserved");
		// 	}
  //       }
		return true;
	}

	public int queryLocationPopularity(int xid, String location)
	{
		// int numCars = car_Manager.queryLocationPopularity(xid, location);
		// int numRooms = room_Manager.queryLocationPopularity(xid, location);
		// return (numCars + numRooms);
		return 0;
	}

	public String getName()
	{
		return m_name;
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (new Integer(string)).intValue();
	}

	public static boolean toBoolean(String string)// throws Exception
	{
		return (new Boolean(string)).booleanValue();
	}
}
