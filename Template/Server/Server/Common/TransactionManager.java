package Server.Common;

import Server.Interface.*;
import Server.LockManager.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class TransactionManager //implements IResourceManager
{

    private static HashMap activeTransactions = new HashMap();
    private static int xid_counter = 0;

	public TransactionManager(String p_name)
	{
		super();
	}

    public int start(IResourceManager rms[]) {
        activeTransactions.put(get_xid(), rms);
        return xid_counter;
    }

    public boolean commit(int transactionId) 
    throws RemoteException {//}, TransactionAbortedException, InvalidTransactionException {
        try {
            for (IResourceManager rm : (IResourceManager[])activeTransactions.get(transactionId)) { 
                rm.commit(transactionId);
            }
            activeTransactions.remove(transactionId);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void abort(int transactionId) 
    throws RemoteException {//}, InvalidTransactionException {
        try {
            for (IResourceManager rm : (IResourceManager[])activeTransactions.get(transactionId)) { 
                rm.abort(transactionId);
            }
            activeTransactions.remove(transactionId);
            return;
        }
        catch (Exception e) {
            return;
        }
    }

    private int get_xid() {
        return ++xid_counter;
    }
}
 
