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
    
    private static final String MAIN = "https://nodes.iota.cafe:443";
    
    private static final String BILL_ICC = "https://78.46.137.210:14265";
    
    private static final String GAL_ICC = "http://gal01.icc.iota.cafe:14265"; //159.69.145.21:

    private static final String TRINITY = "https://nodes.thetangle.org:443"; // https://nodes.iota.cafe:443 -> 0.415
    
    private static final String node = TRINITY;

    public static void main(String[] args) throws MalformedURLException {
        IotaAPI api = new IotaAPI.Builder().addNode(new HttpConnector(new URL(node))).build();
        TestRunner runner = new TestRunner(api);

        runner.run(Tests.PropagationTest);
    }

}
