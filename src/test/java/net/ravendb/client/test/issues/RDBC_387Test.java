package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RDBC_387Test extends RemoteTestBase {

    @Test
    public void shouldStoreSingleId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            String userIdViaSessionId;
            String userIdViaBulkInsert;

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setName("John");
                session.store(u);
                session.saveChanges();
                userIdViaSessionId = u.getId();
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User u = new User();
                u.setName("Marcin");
                bulkInsert.store(u);
                userIdViaBulkInsert = u.getId();
            }

            try (IDocumentSession session = store.openSession()) {
                ObjectNode sessionUser = session.load(ObjectNode.class, userIdViaSessionId);
                ObjectNode bulkInsertUser = session.load(ObjectNode.class, userIdViaBulkInsert);

                assertThat(sessionUser.get("id"))
                        .isNull();
                assertThat(sessionUser.get("Id"))
                        .isNull();

                assertThat(bulkInsertUser.get("id"))
                        .isNull();
                assertThat(bulkInsertUser.get("Id"))
                        .isNull();
            }
        }
    }
}
