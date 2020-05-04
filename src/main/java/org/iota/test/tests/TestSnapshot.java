package org.iota.test.tests;

import org.iota.jota.dto.response.*;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicInteger msIndex = new AtomicInteger();

    private AtomicBoolean newMsIssued = new AtomicBoolean(false);
    
    private List<String> attachedHashes = new ArrayList<>();
    
    // Contains hashes of before the snapshot taken
    private Map<Integer, List<String>> previousHashes = new HashMap<>();
    
    private Transaction transaction;

    private int mwm;

    private int lastSnapshotIndex;

    // Delay between milestones we take a snapshot
    private int snapshotDelay;

    @Override
    public void run() {
        // Step 1
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
        
        //new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkConfirmationAndGTTA, 15, 30, TimeUnit.SECONDS);
    }
    
    // STEP 2 AND 3
    
    private void checkMilestone() {
        try {
            GetNodeInfoResponse info = api.getNodeInfo();
            String ms = info.getLatestMilestone();
            if (msHash.get() == null || !ms.equals(msHash.get())) {
                msHash.set(ms);
                msIndex.set(info.getLatestMilestoneIndex());
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
    
    // STEP 4.A
    
    private void checkTransactions() {
        try {
            int currentSnapshotIndex = this.lastSnapshotIndex;
            Set<String> entrypoints = getEntryPoints();
            if (currentSnapshotIndex != this.lastSnapshotIndex && currentSnapshotIndex != 0) {
                this.snapshotDelay = this.lastSnapshotIndex - currentSnapshotIndex;
                System.out.println("Took snapshot at " + currentSnapshotIndex);
                System.out.println("Current milestones at " + this.msIndex);
                System.out.println("Taking snapshots every " + this.snapshotDelay);
                
                int msWait = this.msIndex.get() - this.lastSnapshotIndex - this.previousHashes.size() * this.snapshotDelay;
                if (msWait > 0) {
                    System.out.println("So we need " + msWait + " more milestones before activating");
                } else {
                    // We took a snapshot! step 4a
                    System.out.println("Entrypoints: " + entrypoints);
                    synchronized (lock) {
                        List<String> previousHashes = this.previousHashes.get(currentSnapshotIndex);
                        if (previousHashes != null) {
                            for (String hash : previousHashes) {
                                if (entrypoints.contains(hash)) {
                                    System.out.println("Were in the SEPS! " + hash);
                                }
                            }
                            checkConfirmationAndGTTA(currentSnapshotIndex);
                        }
                    }
                }
                
                previousHashes.put(this.lastSnapshotIndex, attachedHashes);
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
    
    // STEP 4.B
    
    private void checkConfirmationAndGTTA(int indexToCheck) {
        synchronized (lock) {
            try {
                // Step 4b
                CheckConsistencyResponse response = api.checkConsistency(previousHashes.get(indexToCheck).toArray(new String[0]));
                System.out.println("All confirmed: " + response.getState());
                if (!response.getState()) {
                    System.out.println(response.getInfo());
                }
                
                if (!checkTipselNotAllowed(indexToCheck)) {
                    System.out.println("Whoa we were allowed to use them!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkTipselNotAllowed(int indexToCheck) {
        boolean failed = true;
        for (String hash : previousHashes.get(indexToCheck)) {
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
}
