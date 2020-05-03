package org.iota.test.tests;

import org.iota.jota.dto.response.GetTransactionsToApproveResponse;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CheckTips extends CoreTest {

    private AtomicReference<String> msHash = new AtomicReference<String>(null);

    private AtomicInteger count, equal;

    private AtomicReference<Set<String>> tips = new AtomicReference<>(new HashSet<String>());

    @Override
    public void run() {
        count = new AtomicInteger(0);
        equal = new AtomicInteger(0);

        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(this::updateHash, 0, 10, TimeUnit.SECONDS);

        new ScheduledThreadPoolExecutor(5).scheduleAtFixedRate(this::checkTips, 1, 1, TimeUnit.SECONDS);
    }

    private void updateHash() {
        try {
            String ms = api.getNodeInfo().getLatestMilestone();
            if (msHash.get() == null) {
                msHash.set(ms);
                return;
            }

            if (!ms.equals(msHash.get())) {
                msHash.set(ms);

                System.out.println("Final distinct tips: " + tips.get().size());
                System.out.println("Final percentage: " + Math.round(equal.get() / count.get() * 100));
                tips.get().clear();
                count.set(0);
                equal.set(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkTips() {
        if (msHash.get() == null) {
            return;
        }

        GetTransactionsToApproveResponse res = api.getTransactionsToApprove(1, msHash.get());
        double percentage = count.incrementAndGet();
        if (res.getBranchTransaction().equals(res.getTrunkTransaction())) {
            percentage = (int) Math.round(equal.incrementAndGet() / percentage * 100);
            System.out.println(percentage + " [" + count + " / " + equal + "]");
        }

        if (tips.get().add(res.getTrunkTransaction())) {
            System.out.println(res.getTrunkTransaction());
        }

        if (tips.get().add(res.getBranchTransaction())) {
            System.out.println(res.getBranchTransaction());
        }
    }
}
