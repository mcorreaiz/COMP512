package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Coordination implements Serializable
{
	private static final long serialVersionUID = 1L;
	public Integer highestXid;
	public HashMap<Integer, String> transactionInfo;
	public ArrayList<Integer> abortedT;
	public int crashMode = 0;

	public Coordination(Integer xid, HashMap<Integer, String> tInfo, ArrayList<Integer> taborted, int cm)
	{
		highestXid = Integer.valueOf(xid.intValue());
		transactionInfo = (HashMap<Integer, String>)tInfo.clone();
		abortedT = (ArrayList<Integer>)taborted.clone();
		crashMode = cm;
	}
}
