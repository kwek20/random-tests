package org.iota.test.tests;

import org.iota.jota.model.Neighbor;

public class NodeInfo extends CoreTest {

	@Override
    public void run() {
		System.out.println(api.getNodeInfo());
		System.out.println(api.getNodeAPIConfiguration());
		
		for (Neighbor neighbor : api.getNeighbors().getNeighbors()) {
		    System.out.println("address: " + neighbor.getAddress());
		    System.out.println("all: " + neighbor.getNumberOfAllTransactions());
            System.out.println("invalid: " + neighbor.getNumberOfInvalidTransactions());
            System.out.println("new: " + neighbor.getNumberOfNewTransactions());
            System.out.println("random: " + neighbor.getNumberOfRandomTransactionRequests());
            System.out.println("sent: " + neighbor.getNumberOfSentTransactions());
		}
	}
}
