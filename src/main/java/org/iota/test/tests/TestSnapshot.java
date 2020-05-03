package org.iota.test.tests;

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
    
    private Transaction transaction;

    private int mwm;

    @Override
    public void run() {
        List<Transfer> transfers = new LinkedList<>();
        transfers.add(new Transfer(Constants.NULL_HASH + 999999999, 0, 
                TrytesConverter.asciiToTrytes("Were testing solid entry points, how awesome. " + new Date()), 
                "TEST9TX9NEW9SEP"));
        
        boolean testnet = api.getNodeAPIConfiguration().isTestNet();
        this.mwm = testnet ? 9 : 14;
        SendTransferResponse transferResponse = api.sendTransfer(Constants.NULL_HASH, 1, 1, mwm, transfers, null, null, false, false, null);
        transaction = transferResponse.getTransactions().get(0);
        
        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkMilestone, 0, 10, TimeUnit.SECONDS);

        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::checkTransactions, 0, 10, TimeUnit.SECONDS);
    }

    private void checkMilestone() {
        try {
            String ms = api.getNodeInfo().getLatestMilestone();
            if (msHash.get() == null || !ms.equals(msHash.get())) {
                System.out.println("Running stitch");
                msHash.set(ms);
                newMsIssued.set(true);
                stitch();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stitch() {
        GetAttachToTangleResponse stitch = api.attachToTangle(msHash.get(), transaction.getHash(), mwm, transaction.toTrytes());
        System.out.println(stitch);
        synchronized (lock) {
            attachedHashes.add(Transaction.asTransactionObjects(stitch.getTrytes())[0].getHash());
        }
    }

    private void checkTransactions() {
        try {
            Set<String> entrypoints = getEntryPoints();
            System.out.println("Entrypoints: " + entrypoints);
            synchronized (lock) {
                for (String hash : attachedHashes) {
                    if (entrypoints.contains(hash)) {
                        System.out.println("Were in the SEPS! " + hash);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Set<String> getEntryPoints(){
        Set<String> entryPoints = new HashSet<String>();
        IotaCustomResponse res = api.callIxi("sep.getSep", null);
        Object seps = res.getIxi().get("sep");
        try {
           //Map<String, Integer> json = JsonParser.get().parsJson(seps.toString());
           //System.out.println(json.keySet());
            String map = seps.toString().substring(seps.toString().indexOf("{") + 1, seps.toString().indexOf("}"));
            String[] entries = map.split(", ");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                entryPoints.add(keyValue[0]);
            }
           return entryPoints;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
}
