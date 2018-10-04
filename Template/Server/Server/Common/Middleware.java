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

	private String TCPSend(String cmd, String name) {
		String response = "false";
		try
		{
			TCPConnection resourceManager = new TCPConnection((String)rm_connections.get(name),my_serverPort,name);
			response = resourceManager.sendCommand(cmd);
		}
		catch (IOException e)
		{
			System.out.println("TCP connection error");
		}
		return response;
	}


	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price)
	{
		System.out.println("Middleware add Cars");
		Trace.info("Middleware::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		boolean response;
		String cmd = String.format("addCars,%d,%s,%d,%d", xid, location, count, price);
		response = toBoolean(TCPSend(cmd, "Cars"));
		
		if (response)
		{
			System.out.println("Cars added");
		}
		else
		{
			System.out.println("Cars could not be added");
		}
		return response;
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
	{
		Trace.info("Middleware::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		boolean response;
		String cmd = String.format("addFlight,%d,%d,%d,%d", xid, flightNum, flightSeats, flightPrice);
		response = toBoolean(TCPSend(cmd, "Flights"));
		
		if (response)
		{
			System.out.println("Flight added");
		} else {
			System.out.println("Flight could not be added");
		}

		return response;
	}


	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price)
	{
		Trace.info("Middleware::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		boolean response;
		String cmd = String.format("addRooms,%d,%s,%d,%d", xid, location, count, price);
		response = toBoolean(TCPSend(cmd, "Rooms"));
		
		if (response) {
			System.out.println("Rooms added");
		} else {
			System.out.println("Rooms could not be added");
		}
		return response;
	}

	public int newCustomer(int xid)
	{
		Trace.info("Middleware::newCustomer(" + xid + ") called");
		int customer;
		String cmd = String.format("newCustomer,%d", xid);
		customer = toInt(TCPSend(cmd, "Customers"));
		
		return customer;
	}

	public boolean newCustomer(int xid, int customerID)
	{
		Trace.info("Middleware::newCustomer(" + xid + ", " + customerID + ") called");
		boolean response;
		String cmd = String.format("newCustomer,%d,%d", xid, customerID);
		response = toBoolean(TCPSend(cmd, "Customers"));
		
		if (response)
		{
			System.out.println("Add customer ID: " + customerID);
		} else {
			System.out.println("Customer could not be added");
		}
		return response;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum)
	{
		Trace.info("Middleware::deleteFlight(" + xid + ", " + flightNum + ") called");
		boolean response;
		String cmd = String.format("deleteFlight,%d,%d", xid, flightNum);
		response = toBoolean(TCPSend(cmd, "Flights"));
		
		if (response)
		{
			System.out.println("Flight Deleted");
		} else {
			System.out.println("Flight could not be deleted");
		}
		return response;
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location)
	{
		Trace.info("Middleware::deleteCars(" + xid + ", " + location + ") called");
		boolean response;
		String cmd = String.format("deleteCars,%d,%s", xid, location);
		response = toBoolean(TCPSend(cmd, "Cars"));
		
		if (response)
		{
			System.out.println("Cars Deleted");
		} else {
			System.out.println("Cars could not be deleted");
		}
		return response;
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location)
	{
		Trace.info("Middleware::deleteRooms(" + xid + ", " + location + ") called");
		boolean response;
		String cmd = String.format("deleteRooms,%d,%s", xid, location);
		response = toBoolean(TCPSend(cmd, "Rooms"));

		if (response)
		{
			System.out.println("Rooms Deleted");
		} else {
			System.out.println("Rooms could not be deleted");
		}
		return response;
	}

	public boolean deleteCustomer(int xid, int customerID)
	{
		Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") called");
		boolean response;
		String cmd = String.format("deleteCustomer,%d,%d", xid, customerID);
		response = toBoolean(TCPSend(cmd, "Customers"));

		if (response)
		{
			System.out.println("Customer Deleted");
		} else {
			System.out.println("Customer could not be deleted");
		}
		return response;
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum)
	{
		Trace.info("Middleware::queryFlight(" + xid + ", " + flightNum + ") called");
		int seats;
		String cmd = String.format("queryFlight,%d,%d", xid, flightNum);
		seats = toInt(TCPSend(cmd, "Flights"));

		return seats;
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location)
	{
		Trace.info("Middleware::queryCars(" + xid + ", " + location + ") called");
		int response;
		String cmd = String.format("queryCars,%d,%s", xid, location);
		response = toInt(TCPSend(cmd, "Cars"));

		return response;
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location)
	{
		Trace.info("Middleware::queryRooms(" + xid + ", " + location + ") called");
		int response;
		String cmd = String.format("queryRooms,%d,%s", xid, location);
		response = toInt(TCPSend(cmd, "Rooms"));

		return response;
	}

	public String queryCustomerInfo(int xid, int customerID)
	{
		Trace.info("Middleware::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		String response;
		String cmd = String.format("queryCustomerInfo,%d,%d", xid, customerID);
		response = TCPSend(cmd, "Customers");

		return response;
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum)
	{
		Trace.info("Middleware::queryFlightPrice(" + xid + ", " + flightNum + ") called");
		int response;
		String cmd = String.format("queryFlightPrice,%d,%d", xid, flightNum);
		response = toInt(TCPSend(cmd, "Flights"));

		return response;
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location)
	{
		Trace.info("Middleware::queryCarsPrice(" + xid + ", " + location + ") called");
		int response;
		String cmd = String.format("queryCarsPrice,%d,%s", xid, location);
		response = toInt(TCPSend(cmd, "Cars"));

		return response;
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location)
	{
		Trace.info("Middleware::queryRoomsPrice(" + xid + ", " + location + ") called");
		int response;
		String cmd = String.format("queryRoomsPrice,%d,%s", xid, location);
		response = toInt(TCPSend(cmd, "Rooms"));

		return response;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum)
	{
		Trace.info("Middleware::reserveFlight(" + xid + ", " + customerID + ", " + flightNum + ") called");
		boolean response;
		String cmd = String.format("reserveFlight,%d,%d,%d", xid, customerID, flightNum);
		response = toBoolean(TCPSend(cmd, "Customers"));

		if (response)
		{
			System.out.println("Flight Reserved");
		} else {
			System.out.println("Flight could not be reserved");
		}
		return response;
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location)
	{
		Trace.info("Middleware::reserveCar(" + xid + ", " + customerID + ", " + location + ") called");
		boolean response;
		String cmd = String.format("reserveCar,%d,%d,%s", xid, customerID, location);
		response = toBoolean(TCPSend(cmd, "Customers"));

		if (response)
		{
			System.out.println("Car Reserved");
		} else {
			System.out.println("Car could not be reserved");
		}
		return response;
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location)
	{
		Trace.info("Middleware::reserveRoom(" + xid + ", " + customerID + ", " + location + ") called");
		boolean response;
		String cmd = String.format("reserveRoom,%d,%d,%s", xid, customerID, location);
		response = toBoolean(TCPSend(cmd, "Customers"));

		if (response)
		{
			System.out.println("Room Reserved");
		} else {
			System.out.println("Room could not be reserved");
		}
		return response;
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room)
	{
		Trace.info("Middleware::bundle(" + xid + ", " + customerId + ", " + flightNumbers + "," + location + "," + car + "," + room + ") called");
		//first reserve flights
		String[] flightNums = new String[flightNumbers.size()];
		flightNums = (String[]) flightNumbers.toArray(flightNums);
		boolean response;

		for (int i = 0; i < flightNums.length; i++)
        {
        	int flightNum = toInt(flightNums[i]);
        	Trace.info("Middleware::reserveFlight(" + xid + ", " + customerId + ", " + flightNum + ") in bundle");
			String cmd = String.format("reserveFlight,%d,%d,%d", xid, customerId, flightNum);
			response = toBoolean(TCPSend(cmd, "Customers"));
	
			if (response)
			{
			System.out.println("Flight Reserved");
			}
			else {
				System.out.println("Flight could not be reserved");
			}
        }

        //reserve optional rooms & cars
        if(car==true)
        {
        	Trace.info("Middleware::reserveCar(" + xid + ", " + customerId + ", " + location + ") in bundle");
			String cmd = String.format("reserveCar,%d,%d,%s", xid, customerId, location);
			response = toBoolean(TCPSend(cmd, "Customers"));
	
			if (response)
			{
				System.out.println("Car Reserved");
			}
			else
			{
				System.out.println("Car could not be reserved");
			}
        }

        if(room==true)
        {
        	Trace.info("Middleware::reserveRoom(" + xid + ", " + customerId + ", " + location + ") in bundle");
			String cmd = String.format("reserveRoom,%d,%d,%s", xid, customerId, location);
			response = toBoolean(TCPSend(cmd, "Customers"));
	
			if (response)
        	{
        		System.out.println("Room Reserved");
			}
			else
			{
				System.out.println("Room could not be reserved");
			}
        }
		return true;
	}

	public int queryLocationPopularity(int xid, String location)
	{
		Trace.info("Middleware::queryLocationPopularity(" + xid + ", " + location + ")");
		String cmd = String.format("queryLocationPopularity,%d,%s", xid, location);
		int numCars = toInt(TCPSend(cmd, "Cars"));
		int numRooms = toInt(TCPSend(cmd, "Rooms"));

		return (numCars + numRooms);
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
