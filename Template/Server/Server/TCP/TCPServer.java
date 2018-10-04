package Server.TCP;
import java.io.*;
import java.net.*;
import java.util.*;
//sets up a TCP connection and executes one command


class TCPServer extends Thread{

  private static int serverPort = 3000;
  private Socket connectionSocket;
  private TCPMiddleware service;

  public TCPServer(Socket connection, TCPMiddleware middleware)
  {
    this.connectionSocket = connection;
    service = middleware;
  }

  public void run()
  {
    try{
      really_runs();
    }
    catch (Exception e){

    }
  }

  public void really_runs() throws Exception
  {
    String clientSentence;
    String outMessage;

    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
    clientSentence = inFromClient.readLine();
    System.out.println("Received: " + clientSentence);
    outMessage = parse_execute(clientSentence);
    outToClient.writeBytes(outMessage + '\n');
    connectionSocket.close();
  }

  public String parse_execute(String input)
  {
    input = input.toLowerCase();
    List<String> arguments = Arrays.asList(input.split("\\s*,\\s*"));
    int arg_size = arguments.size();
    String[] args = new String[arg_size];
    for (int i=0; i<arg_size; i++)
    {
      args[i] = arguments.get(i);
    }

    if (args[0].equals("connectiontest"))
    {
      return "online";
    }
    else if (args[0].equals("addcars"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int count = toInt(args[3]);
      int price = toInt(args[4]);
      boolean success = service.addCars(xid, location, count, price);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("addflight"))
    {
      int xid = toInt(args[1]);
      int flightNum = toInt(args[2]);
      int flightSeats = toInt(args[3]);
      int flightPrice = toInt(args[4]);
      boolean success = service.addFlight(xid, flightNum, flightSeats, flightPrice);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }

    }
    else if (args[0].equals("addrooms"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int count = toInt(args[3]);
      int price = toInt(args[4]);
      boolean success = service.addRooms(xid, location, count, price);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("newcustomer") && (arg_size == 2))
    {
      int xid = toInt(args[1]);
      int cid = service.newCustomer(xid);
      return (Integer.toString(cid));
    }
    else if (args[0].equals("newcustomer") && (arg_size == 2))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      boolean success = service.newCustomer(xid, customerID);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("deleteflight"))
    {
      int xid = toInt(args[1]);
      int flightNum = toInt(args[2]);
      boolean success = service.deleteFlight(xid, flightNum);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("deletecars"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      boolean success = service.deleteCars(xid, location);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("deleterooms"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      boolean success = service.deleteRooms(xid, location);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("deletecustomer"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      boolean success = service.deleteCustomer(xid, customerID);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("queryflight"))
    {
      int xid = toInt(args[1]);
      int flightNum = toInt(args[2]);
      int seats = service.queryFlight(xid, flightNum);
      return (Integer.toString(seats));
    }
    else if (args[0].equals("querycars"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int avail = service.queryCars(xid, location);
      return (Integer.toString(avail));
    }
    else if (args[0].equals("queryrooms"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int avail = service.queryRooms(xid, location);
      return (Integer.toString(avail));
    }
    else if (args[0].equals("querycustomerinfo"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      String output = service.queryCustomerInfo(xid, customerID);
      return output;
    }
    else if (args[0].equals("queryflightprice"))
    {
      int xid = toInt(args[1]);
      int flightNum = toInt(args[2]);
      int price = service.queryFlightPrice(xid, flightNum);
      return (Integer.toString(price));
    }
    else if (args[0].equals("querycarsprice"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int price = service.queryCarsPrice(xid, location);
      return (Integer.toString(price));
    }
    else if (args[0].equals("queryroomsprice"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int price = service.queryRoomsPrice(xid, location);
      return (Integer.toString(price));
    }
    else if (args[0].equals("reserveflight"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      int flightNum = toInt(args[3]);
      boolean success = service.reserveFlight(xid, customerID, flightNum);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("reservecar"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      String location = args[3];
      boolean success = service.reserveCar(xid, customerID, location);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("reserveroom"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      String location = args[3];
      boolean success = service.reserveRoom(xid, customerID, location);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("bundle"))
    {
      int xid = toInt(args[1]);
      int customerID = toInt(args[2]);
      int numFlights = toInt(args[3]);
      Vector<String> flightNumbers = new Vector<String>(numFlights);
      for (int i=0; i<numFlights; i++)
      {
        flightNumbers.addElement(args[i+3]);
      }
      String location = args[3+numFlights];
      boolean car = toBoolean(args[3+numFlights+1]);
      boolean room = toBoolean(args[3+numFlights+2]);
      boolean success = service.bundle(xid, customerID, flightNumbers, location, car, room);
      if (success)
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }
    else if (args[0].equals("querylocationpopularity"))
    {
      int xid = toInt(args[1]);
      String location = args[2];
      int numReserve = service.queryLocationPopularity(xid, location);
      return (Integer.toString(numReserve));
    }
    else
    {
      return "unknown";
    }
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
