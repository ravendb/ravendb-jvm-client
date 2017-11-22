package net.ravendb.client.test.client.issues;

import jdk.nashorn.internal.ir.annotations.Ignore;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.indexes.CompactIndexOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class RavenDB_5435Test extends RemoteTestBase {
    public static class Users_ByName extends AbstractIndexCreationTask {
        public Users_ByName() {
            map = "from u in docs.Users select new { u.Name }";
        }
    }

    @Test
    @Ignore
    public void canCompact() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 1000; i++)
                {
                    User user = new User();
                    user.setName(String.valueOf(i));

                    session.store(user);
                }

                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            Operation operation = store.admin().sendOperation(new CompactIndexOperation(new Users_ByName().getIndexName()));
            operation.waitForCompletion();
        }
    }

}
