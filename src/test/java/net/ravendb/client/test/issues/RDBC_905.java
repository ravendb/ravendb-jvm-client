package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import org.junit.jupiter.api.Test;

public class RDBC_905 extends RemoteTestBase {

    @Test
    public void canOpenBrowserOnMacOs() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            waitForUserToContinueTheTest(store);
        }
    }
}