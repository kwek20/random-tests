package org.iota.test.tests;

import org.iota.jota.IotaAPI;

public class NodeInfo extends CoreTest {

	public void run() {
		System.out.println(api.getNodeInfo());
	}
}
