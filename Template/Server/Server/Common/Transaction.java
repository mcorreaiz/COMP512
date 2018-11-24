package Server.Common;

public class Transaction
{
    public int xid;
    public String logStatus;
    public RMHashMap data;

	public Transaction(int xid)
	{
		xid = xid;
	}
}
