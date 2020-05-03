package org.iota.test.tests;

import org.iota.jota.IotaAPI;

public class TestRunner {

	protected IotaAPI api;

	public TestRunner(IotaAPI api) {
		this.api = api;
	}

	public void run(Tests test) {
		test.getInstance().run(api);
	}
}
