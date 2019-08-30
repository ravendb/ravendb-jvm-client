package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.json.MetadataAsDictionary;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RDBC_339Test extends RemoteTestBase {

    @Test
    public void invalidAttachmentsFormat() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setName("John");
                session.store(u);

                session.advanced().attachments().store(u, "data", new ByteArrayInputStream(new byte[]{1, 2, 3}));
                session.saveChanges();

                User u2 = new User();
                u2.setName("Oz");
                session.store(u2);
                session.saveChanges();
            }
        }
    }
}
