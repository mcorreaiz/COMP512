package Server.Common;
import java.util.*;
import java.io.*;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class StoreableRM implements Serializable {
    // private LockManager lockManager = new LockManager();

    // private static HashMap<String, Integer> LocationMapCar = new HashMap<String, Integer>();
    // private static HashMap<String, Integer> LocationMapRoom = new HashMap<String, Integer>();

    private RMHashMap m_data = new RMHashMap();
    private HashSet<Integer> startedTransactions = new HashSet<Integer>();
    private HashSet<Integer> abortedTransactions = new HashSet<Integer>();

    public StoreableRM(RMHashMap data, HashSet<Integer> startedT, HashSet<Integer> abortedT) {
        m_data = data;
        startedTransactions = startedT;
        abortedTransactions = abortedT;
    }

    public RMHashMap getData() {
        return m_data;
    }

    public void addData(String key, RMItem item) {
        m_data.put(key, item);
    }

    public HashSet<Integer> getStartedT() {
        return startedTransactions;
    }

    public HashSet<Integer> getAbortedT() {
        return abortedTransactions;
    }
}