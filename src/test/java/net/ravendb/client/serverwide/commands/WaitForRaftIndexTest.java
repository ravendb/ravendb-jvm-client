package net.ravendb.client.serverwide.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.PutIndexResult;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitForRaftIndexTest extends RemoteTestBase {
    @Test
    public void canWaitForRaftIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("Users_ByName");
            indexDefinition.setMaps(Collections.singleton("from user in docs.Users select new { user.name }"));

            PutIndexResult[] results = store.maintenance().send(new PutIndexesOperation(indexDefinition));
            long raftIndex = results[0].getRaftCommandIndex();

            assertThat(raftIndex)
                    .isPositive();

            store.getRequestExecutor().execute(new WaitForRaftIndexCommand(raftIndex));
            store.getRequestExecutor().execute(new WaitForRaftIndexCommand(raftIndex, "A"));
        }
    }
}
