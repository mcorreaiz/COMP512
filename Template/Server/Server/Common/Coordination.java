package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Coordination implements Serializable
{
	public Integer highestXid;
	public HashMap<Integer, String> transactionInfo;
	public ArrayList<Integer> abortedT;

	public Coordination(Integer xid, HashMap<Integer, String> tInfo, ArrayList<Integer> taborted)
	{
		highestXid = Integer.valueOf(xid.intValue());
		transactionInfo = (HashMap<Integer, String>)tInfo.clone();
		abortedT = (ArrayList<Integer>)taborted.clone();
	}
}
