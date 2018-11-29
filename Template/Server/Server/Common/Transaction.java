package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction implements Serializable
{
	private static final long serialVersionUID = 1L;
	public int xid;
	public String managers;
	public List<String> StatusLog;
	public RMHashMap data;

	public Transaction(int xid)
	{
		this.xid = xid;
		StatusLog = new ArrayList<String>();
		data = null;
		managers = "";
	}

	public void addLog(String status)
	{
		StatusLog.add(status);
	}

	public void setData(RMHashMap _data)
	{
		data = _data;
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

	public void addManager(String manager){
		managers = managers + manager;
	}
}
