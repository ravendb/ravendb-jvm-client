package net.ravendb.client.documents.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.serverwide.operations.ReorderDatabaseMembersOperation;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ReorderDatabaseMembersTest extends RemoteTestBase {

    @Test
    public void canSendReorderCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().server().send(new ReorderDatabaseMembersOperation(store.getDatabase(), Collections.singletonList("A")));
        }
    }
}
