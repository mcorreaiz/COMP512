package Client;

import Server.Interface.*;

import java.util.*;
import java.io.*;

public class AutomatedClient extends Thread
{
    private int numQueries = 1000;
    private int queriesPerSecond = 2;
    public long times[] = new long[numQueries];
	IResourceManager resourceManager = null;

    public AutomatedClient(IResourceManager rm) {
        super();
        resourceManager = rm;
    }

    public void run() {
        for (int i=0; i < numQueries; i++) {
            long startTime = System.nanoTime();

            int xid = resourceManager.start();
            resourceManager.queryCustomer(xid, 1);
            resourceManager.commit(xid);

            long elapsedTime = System.nanoTime() - startTime;
            times[i] = elapsedTime;
            Thread.sleep((1000 / queriesPerSecond) + (int)(Math.random() * 100) - 50 - elapsedTime);
        }
    }
}