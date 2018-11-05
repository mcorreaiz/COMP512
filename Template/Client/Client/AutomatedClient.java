package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class AutomatedClient extends Thread
{
    private int numQueries = 0;
    private int queriesPerSecond = 0;
    public long timeSum = 0;
    //ClientSimulator parent = null;
	IResourceManager resourceManager = null;

    public AutomatedClient(IResourceManager rm,int numQ, int qps) {
        super();
        //parent = cs;
        resourceManager = rm;
        numQueries = numQ;
        queriesPerSecond = qps;
    }

    public void run() {
		try {
            int xid;
            long startTime;
            long elapsedTime;
            long toSleep;
            for (int i=0; i < numQueries; i++) {
                startTime = System.nanoTime();
            
                xid = resourceManager.start();
                resourceManager.newCustomer(xid,xid);
                resourceManager.addFlight(xid,xid,100,100);
                resourceManager.reserveFlight(xid, xid, xid);
                resourceManager.commit(xid);

                elapsedTime = System.nanoTime() - startTime;
                timeSum += elapsedTime;
                
                toSleep = 1000 / queriesPerSecond + (long)(Math.random() * 10) - 5 - elapsedTime / 1000000;
                System.out.println("I am sleeping for "+ Long.toString(toSleep));
                if (toSleep > 0) {
                    Thread.sleep((int)toSleep);
                }
            }
            long average = timeSum / numQueries / 1000000;
            System.out.println("My name is thread number: "+ Long.toString(Thread.currentThread().getId()));
            System.out.println(Long.toString(average));
                

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
