package net.ravendb;

import net.ravendb.client.test.driver.RavenServerLocator;
import net.ravendb.client.test.driver.RavenTestDriver;

public class RemoteTestBase extends RavenTestDriver {

    private static class TestServiceLocator extends RavenServerLocator {
        @Override
        public String getServerPath() {
            return "C:\\temp\\RavenDB-4.0.0-nightly-20171020-0400-windows-x64\\Server\\Raven.Server.exe"; //TODO: from variable?
        }
    }

    public RemoteTestBase() {
        super(new TestServiceLocator());
    }

}
