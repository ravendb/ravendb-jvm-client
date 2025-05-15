package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

public class RDBC_905 extends RemoteTestBase {

    @Disabled("Skipping test")
    @Test
    public void canOpenBrowserOnMacOs() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            waitForUserToContinueTheTest(store);
        }
    }
}