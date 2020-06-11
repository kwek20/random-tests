package org.iota.test;

import org.iota.jota.IotaAPI;
import org.iota.jota.connection.HttpConnector;
import org.iota.test.tests.TestRunner;
import org.iota.test.tests.Tests;

import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    
    private static final String BINANCE = "http://iota.fdgahl.cn:8088";
    
    private static final String node = BINANCE;

    public static void main(String[] args) throws MalformedURLException {
        IotaAPI api = new IotaAPI.Builder().addNode(new HttpConnector(new URL(node))).build();
        TestRunner runner = new TestRunner(api);
        
        runner.run(Tests.NodeInfo);
        System.out.println("--------------");
        runner.run(Tests.FakeTransfer);
    }

}
