package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class AutomatedClient extends Thread
{
    private int numQueries = 0;
    private double queriesPerSecond = 0;
    private int numClients = 0;
    private long timeSum = 0;
    private String tid = "";
    public long timePerQuery = 0;
    //ClientSimulator parent = null;
	IResourceManager resourceManager = null;

    public AutomatedClient(IResourceManager rm,int numQ, double qps, int numC) {
        super();
        //parent = cs;
        resourceManager = rm;
        numQueries = numQ;
        queriesPerSecond = qps;
        numClients = numC;
    }

    public void run() {
		try {
            int xid;
            long startTime;
            long elapsedTime;
            long toSleep;
            tid = Long.toString(Thread.currentThread().getId());;
            for (int i=-15; i < numQueries; i++) {
                startTime = System.nanoTime();
            
                print("Starting new transaction");
                xid = resourceManager.start();
                resourceManager.newCustomer(xid,xid);
                resourceManager.addFlight(xid,xid,100,100);
                resourceManager.reserveFlight(xid, xid, xid);
                resourceManager.commit(xid);

                if (i >= 0) {
                    elapsedTime = (System.nanoTime() - startTime) / 1000000;
                    timeSum += elapsedTime;
                    
                    toSleep = (long)(1000 / queriesPerSecond) - elapsedTime;
                    if (toSleep > 0 && numClients > 1) {
                        print("I am sleeping for "+ Long.toString(toSleep) + " ms");
                        Thread.sleep((int)(toSleep * ((100 + randomWithRange(-5, 5)) / (float)100))); // +- 5%
                    }
                } else {
                    Thread.sleep(100);
                }
            }
            timePerQuery = timeSum / numQueries;
            // print("My average performance was: " + Long.toString(timePerQuery) + "ms");
                

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void print(String toPrint) {
        System.out.println(toPrint + " [tid=" + tid + "]");
    }

    public int randomWithRange(int min, int max)
    {
    int range = Math.abs(max - min) + 1;     
    return (int)(Math.random() * range) + (min <= max ? min : max);
    }
}
