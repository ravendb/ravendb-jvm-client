package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_19146Test extends RemoteTestBase {

    @Test
    public void can_Use_StartsWith_In_Empty_Collection() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "test";

            try (IDocumentSession session = store.openSession()) {
                User expando = new User();
                session.store(expando, id);

                IMetadataDictionary metadata = session.advanced().getMetadataFor(expando);
                metadata.put(Constants.Documents.Metadata.COLLECTION, null);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> results = session.advanced().rawQuery(ObjectNode.class, "from @empty where startsWith(id(), 't')")
                        .toList();

                assertThat(results)
                        .hasSize(1);
            }
        }
    }
}
