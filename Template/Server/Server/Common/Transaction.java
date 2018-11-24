package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction implements Serializable
{
    public int xid;
    public String logStatus;
    public RMHashMap data;

	public Transaction(int xid)
	{
		this.xid = xid;
	}
}
