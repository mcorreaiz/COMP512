package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class AutomatedClient extends Thread
{
    private int numQueries = 1000;
    private int queriesPerSecond = 2;
    public long timeSum = 0;
    ClientSimulator parent = null;
	IResourceManager resourceManager = null;

    public AutomatedClient(ClientSimulator cs, IResourceManager rm, int qps) {
        super();
        parent = cs;
        resourceManager = rm;
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
                resourceManager.reserveFlight(xid, 1, 9090);
                resourceManager.commit(xid);

                elapsedTime = System.nanoTime() - startTime;
                timeSum += elapsedTime;
                
                toSleep = (1000 / queriesPerSecond) + ((int)(Math.random() * 100) - 50) - elapsedTime;
                if (toSleep > 0) {
                    Thread.sleep(toSleep);
                }
                long average = timeSum / numQueries;
                parent.threadDone(average);
            }

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
