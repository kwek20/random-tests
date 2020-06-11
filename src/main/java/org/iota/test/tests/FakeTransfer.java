package org.iota.test.tests;

import org.iota.jota.model.Input;
import org.iota.jota.model.Transaction;
import org.iota.jota.model.Transfer;
import org.iota.jota.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakeTransfer extends CoreTest {
    
    private final static int MWM = 14;
    private final static int DEPTH = 3;
    
    @Override
    public void run() {
        String seed = Constants.NULL_HASH;
        int security = 3;
        String fromAddress = "SVWPALDGVG9LKWUO9LBWINCCGQFGVZTSKEVCH9BLOKFEWAMEVEVNZMQLRMQDQQEDKCZIDAJVMGRXGACZCWMWTUXMPZ";//index 4700
        String toAddress = "KHBFIUKVKWOCIHZDOKFG9MPYXWESMHFQYOSSZJUBRFKDHPFDTJEHLYPCXDOPDHXXR9MJJPYSRREQBOGOXBKITCVVDY";
        // outputs
        long balance = 50;//api.getBalance(100, fromAddress);
        List<Transfer> transfers = new ArrayList<>();
        transfers.add(new Transfer(fromAddress, balance, "999999999999999999999999999", ""));
        // inputs
        List<Input> inputs = new ArrayList<>();
        inputs.add(new Input(fromAddress, balance,4700, security));
        final List<String> trytes = api.prepareTransfers(seed, security, transfers, toAddress, inputs, null, false);
        List<Transaction> trxs = api.sendTrytes(trytes.toArray(new String[0]), DEPTH, MWM, null);
        System.out.println("tx Size:" + trxs.size());
        System.out.println(Arrays.toString(trxs.toArray()));
    }
}
