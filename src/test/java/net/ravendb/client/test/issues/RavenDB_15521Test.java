package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15521Test extends RemoteTestBase {

    @Test
    public void shouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                SimpleDoc doc = new SimpleDoc();
                doc.setId("TestDoc");
                doc.setName("State1");

                session.store(doc);

                String attachment = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
                session
                        .advanced()
                        .attachments()
                        .store(doc, "TestAttachment", new ByteArrayInputStream(attachment.getBytes()));

                session.saveChanges();

                String changeVector1 = session.advanced().getChangeVectorFor(doc);
                session.advanced().refresh(doc);
                String changeVector2 = session.advanced().getChangeVectorFor(doc);
                assertThat(changeVector2)
                        .isEqualTo(changeVector1);

            }
        }
    }

    public static class SimpleDoc {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
