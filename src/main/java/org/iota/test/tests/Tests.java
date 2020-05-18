package org.iota.test.tests;

public enum Tests {
    InspectBundle(InspectBundle.class),
    TestSnapshot(TestSnapshot.class),
    CheckTips(CheckTips.class), 
    NodeInfo(NodeInfo.class), 
    PropagationTest(PropagationTest.class);

	private Class<? extends CoreTest> clazz;

	Tests(Class<? extends CoreTest> clazz) {
		this.clazz = clazz;
	}

	CoreTest getInstance() {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			System.out.println("Test cant run :(");
			e.printStackTrace();
			return null;
		}
	}
}
