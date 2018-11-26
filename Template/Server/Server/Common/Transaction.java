package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction implements Serializable
{
	private static final long serialVersionUID = 1L;
    public int xid;
	public List<String> StatusLog;
	public RMHashMap data;

	public Transaction(int xid)
	{
		this.xid = xid;
		StatusLog = new ArrayList<String>();
		data = null;
	}

	public void addLog(String status)
	{
		StatusLog.add(status);
	}

	public String latestLog()
	{
		if(StatusLog.size() == 0){
			return "Empty";
		}
		else{
			return StatusLog.get(StatusLog.size()-1);
		}
	}
}
