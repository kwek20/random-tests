package org.iota.test.tests;

import org.iota.jota.IotaAPI;

public abstract class CoreTest implements Runnable {

	protected IotaAPI api;

	void run(IotaAPI api) {
		this.api = api;
		run();
	}
}
