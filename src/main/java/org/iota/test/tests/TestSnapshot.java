package org.iota.test.tests;

import org.iota.jota.dto.response.CheckConsistencyResponse;
import org.iota.jota.dto.response.GetAttachToTangleResponse;
import org.iota.jota.dto.response.IotaCustomResponse;
import org.iota.jota.dto.response.SendTransferResponse;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
So what you need to do is:
1. create a transaction that is not confirmed that attaches to a milestone
2. Every time a new milestone comes in create a stitching transaction that approves the tx from (1) and the new milestone
3. repeatz
4. after a snapshot takes place of a milestone that is above some of those transactions ensure:
a. that your txs are not in the SEP set
b. your txs are not eligible for tipsel
c. you can sync a new node with the LS with ease :-)
*/
public class TestSnapshot extends CoreTest {
    
    private Object lock = new Object();

    private AtomicReference<String> msHash = new AtomicReference<String>(null);

    private AtomicBoolean newMsIssued = new AtomicBoolean(false);
    
    private List<String> attachedHashes = new ArrayList<>();
    private List<String> previousHashes = new ArrayList<>();
    
    private Transaction transaction;

    private int mwm;

    private int lastSnapshotIndex;

    @Override
    public void run() {
        List<Transfer> transfers = new LinkedList<>();
        transfers.add(new Transfer(Constants.NULL_HASH + "999999999", 0, 
                TrytesConverter.asciiToTrytes("Were testing solid entry points, how awesome. " + new Date()), 
                "TEST9TX9NEW9SEP"));
        
        boolean testnet = api.getNodeAPIConfiguration().isTestNet();
        this.mwm = testnet ? 9 : 14;
        SendTransferResponse transferResponse = api.sendTransfer(Constants.NULL_HASH, 1, 1, mwm, transfers, null, null, false, false, null);
        transaction = transferResponse.getTransactions().get(0);
        
        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkMilestone, 0, 10, TimeUnit.SECONDS);

        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkTransactions, 0, 10, TimeUnit.SECONDS);
        
        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkConfirmationAndGTTA, 15, 30, TimeUnit.SECONDS);
    }
    
    private void checkConfirmationAndGTTA() {
        synchronized (lock) {
            try {
                // Step 4b
                CheckConsistencyResponse response = api.checkConsistency(attachedHashes.toArray(new String[0]));
                System.out.println("All confirmed: " + response.getState());
                if (!response.getState()) {
                    System.out.println(response.getInfo());
                }
                
                if (!checkTipselNotAllowed()) {
                    System.out.println("Whoa we were allowed to use them!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkTipselNotAllowed() {
        boolean failed = true;
        for (String hash : attachedHashes) {
            try {
                api.attachToTangle(hash, hash, mwm, transaction.toTrytes());
                
                failed = false;
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return failed;
    }

    private void checkMilestone() {
        try {
            String ms = api.getNodeInfo().getLatestMilestone();
            if (msHash.get() == null || !ms.equals(msHash.get())) {
                msHash.set(ms);
                newMsIssued.set(true);

                //Attach our tx to current milestone, step 2 and 3
                stitch();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stitch() {
        GetAttachToTangleResponse stitch = api.attachToTangle(msHash.get(), transaction.getHash(), mwm, transaction.toTrytes());
        api.storeAndBroadcast(stitch.getTrytes());
        synchronized (lock) {
            Transaction tx = new Transaction();
            tx.transactionObject(stitch.getTrytes()[0]);
                    
            attachedHashes.clear();
            attachedHashes.add(tx.getHash());
        }
    }

    private void checkTransactions() {
        try {
            int currentSnapshotIndex = this.lastSnapshotIndex;
            Set<String> entrypoints = getEntryPoints();
            if (currentSnapshotIndex != this.lastSnapshotIndex && this.lastSnapshotIndex != 0) {
                System.out.println("Took snapshot at " + currentSnapshotIndex);
                
                // We took a snapshot! step 4a
                System.out.println("Entrypoints: " + entrypoints);
                synchronized (lock) {
                    for (String hash : previousHashes) {
                        if (entrypoints.contains(hash)) {
                            System.out.println("Were in the SEPS! " + hash);
                        }
                    }
                }
                
                previousHashes.clear();
                previousHashes.addAll(attachedHashes);
                attachedHashes.clear();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Set<String> getEntryPoints(){
        Set<String> entryPoints = new HashSet<String>();
        IotaCustomResponse res = api.callIxi("sep.getSep", null);
        Object index = res.getIxi().get("index");
        lastSnapshotIndex = (int) Double.parseDouble(index.toString());
        
        Object seps = res.getIxi().get("sep");
        try {
            String map = seps.toString().substring(seps.toString().indexOf("{") + 1, seps.toString().indexOf("}"));
            String[] entries = map.split(", ");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                entryPoints.add(keyValue[0]);
            }
           return entryPoints;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
