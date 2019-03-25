package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.serverwide.operations.PromoteDatabaseNodeOperation;
import org.junit.jupiter.api.Test;

public class PromoteDatabaseTest extends RemoteTestBase {

    @Test
    public void canSendPromoteDatabaseCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            PromoteDatabaseNodeOperation operation = new PromoteDatabaseNodeOperation(store.getDatabase(), "A");
            store.maintenance().server().send(operation);

            // since we are running single node cluster we cannot assert much
        }
    }
}
