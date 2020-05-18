package org.iota.test.tests;

import org.iota.jota.IotaAPI;
import org.iota.jota.dto.response.GetAttachToTangleResponse;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PropagationTest extends CoreTest {
    
    public static final String COMPASS = "compass01.mainnet.iota.cafe";
    public static final String TAG = "PROPOGATION9TEST";

    public static final int NUM_TX = 100;
    
    private static ScheduledFuture<?> sendTask, checkTask;
    
    private IotaAPI apiWeCheck;
    
    private Map<String, Long> hashesToCheck = new HashMap<>();
    
    private AtomicInteger counter = new AtomicInteger();
    private AtomicLong avgTimeTotal = new AtomicLong(0);
    
    private List<String> trytes;
    private String EMPTY_TRYTES = "999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999";
    
    public PropagationTest() {
        this.apiWeCheck = new IotaAPI.Builder().host(COMPASS, false).port(43210).protocol("http").build();
    }

	@Override
    public void run() {
	    try {
            apiWeCheck.getNodeInfo();
        } catch (Exception e) {
            System.out.println("Failed to connect to node " + apiWeCheck.getHost());
            e.printStackTrace();
            return;
        }
	    
	    StringBuilder s = new StringBuilder();
	    for (int i=0; i < Constants.TRANSACTION_LENGTH;i++) {
	        s.append("9");
	    }
	    //EMPTY_TRYTES = s.toString();
	    
	    List<Transfer> transfers = new ArrayList<>();
        transfers.add(new Transfer(Constants.NULL_HASH + 999999999, 0, "", TAG));
        trytes = api.prepareTransfers(Constants.NULL_HASH, 1, transfers, null, null, null, false);
	    
	    sendTask = new ScheduledThreadPoolExecutor(10).scheduleAtFixedRate(this::sendBundle, 0, 1, TimeUnit.MILLISECONDS);
	    checkTask = new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkHashes, 1101, 1, TimeUnit.MILLISECONDS);
	}
	
	private void sendBundle() {
	    try {
	        if (counter.get() == NUM_TX) {
	            sendTask.cancel(false);
	            System.out.println("Stopping!");
	            return;
	        }
	       
	        // attach to tangle - do pow
	        GetAttachToTangleResponse res = api.attachToTangle(Constants.NULL_HASH, Constants.NULL_HASH, 14, trytes.toArray(new String[0]));
	        String hash = new Transaction(res.getTrytes()[0]).getHash();
	        
	        long time = System.currentTimeMillis(); 
	        hashesToCheck.put(hash, time);
	        System.out.println("Adding " + hash + " at " + time);
	        
	        api.broadcastTransactions(res.getTrytes());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
	}
	
	private void checkHashes() {
	    if (hashesToCheck.isEmpty()) {
	        return;
	    }
	    try {
    	    for (Entry<String, Long> entry : new HashMap<>(hashesToCheck).entrySet()) {
    	        long time = System.currentTimeMillis();
    	        String trytes = apiWeCheck.getTrytes(entry.getKey()).getTrytes()[0];
    	        if (!trytes.equals(EMPTY_TRYTES)) {
    	            System.out.println("Found " + entry.getKey() + " at " + (entry.getValue() - time));
    	            
    	            int total = counter.incrementAndGet();
    	            long avg = avgTimeTotal.addAndGet(time - entry.getValue()) / total;
    	            System.out.println("new Avg: " + (avg / 1000d) + "s (" + total + "/" + avgTimeTotal.get() + ")");
    	            
    	            if (total == NUM_TX) {
    	                sendTask.cancel(false);
    	            }
    	            
    	            synchronized (hashesToCheck) {
    	                hashesToCheck.remove(entry.getKey());
    	            }
    	        } else {
    	            //System.out.println("didnt find " + entry.getKey());
    	        }
    	    }
    	} catch (Exception e) {
            System.out.println(e.getMessage());
        }
	}
}
