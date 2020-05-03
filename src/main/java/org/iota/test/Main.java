package org.iota.test;

import org.iota.jota.IotaAPI;
import org.iota.jota.connection.HttpConnector;
import org.iota.test.tests.TestRunner;
import org.iota.test.tests.Tests;

import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    
    private static final String ICC = "http://159.69.29.171:14265";

    private static final String DEV = "https://nodes.devnet.iota.org";

    private static final String LOCAL = "http://localhost:14265";

    private static final String node = ICC;

    public static void main(String[] args) throws MalformedURLException {
        IotaAPI api = new IotaAPI.Builder().addNode(new HttpConnector(new URL(node))).build();
        TestRunner runner = new TestRunner(api);

        runner.run(Tests.TestSnapshot);
    }

}
