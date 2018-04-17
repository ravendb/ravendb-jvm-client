package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_10641Test extends RemoteTestBase {

    public static class Document {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }


    @Test
    public void canEditObjectsInMetadata() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Document v = new Document();
                session.store(v, "items/first");

                HashMap<String, String> items = new HashMap<>();
                items.put("lang", "en");

                session.advanced().getMetadataFor(v)
                        .put("Items", items);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Document v = session.load(Document.class, "items/first");
                Map<String, Object> metadata = (Map<String, Object>) session.advanced().getMetadataFor(v).get("Items");
                metadata.put("lang", "sv");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Document v = session.load(Document.class, "items/first");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(v);
                metadata.put("test", "123");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Document v = session.load(Document.class, "items/first");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(v);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Document v = session.load(Document.class, "items/first");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(v);
                assertThat(((Map<String, Object>)metadata.get("Items")).get("lang"))
                        .isEqualTo("sv");

                assertThat(metadata.get("test"))
                        .isEqualTo("123");
            }
        }
    }

}
