package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15531Test extends RemoteTestBase {

    @Test
    public void updateSessionChangesAfterTrackedEntityIsRefreshed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                SimpleDoc doc = new SimpleDoc();
                doc.setId("TestDoc");
                doc.setName("State1");

                session.store(doc);
                session.saveChanges();

                doc.setName("State2");
                Map<String, List<DocumentsChanges>> changes1 =
                        session.advanced().whatChanged();

                List<DocumentsChanges> changes = changes1.get("TestDoc");
                assertThat(changes)
                        .isNotNull()
                        .hasSize(1);

                assertThat(changes.get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get(0).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get(0).getFieldOldValue().toString())
                        .isEqualTo("\"State1\"");
                assertThat(changes.get(0).getFieldNewValue().toString())
                        .isEqualTo("\"State2\"");

                session.saveChanges();

                doc.setName("State3");

                changes1 =
                        session.advanced().whatChanged();

                changes = changes1.get("TestDoc");
                assertThat(changes)
                        .isNotNull()
                        .hasSize(1);

                assertThat(changes.get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get(0).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get(0).getFieldOldValue().toString())
                        .isEqualTo("\"State2\"");
                assertThat(changes.get(0).getFieldNewValue().toString())
                        .isEqualTo("\"State3\"");

                session.advanced().refresh(doc);

                doc.setName("State4");
                changes1 =
                        session.advanced().whatChanged();
                changes = changes1.get("TestDoc");
                assertThat(changes)
                        .isNotNull()
                        .hasSize(1);

                assertThat(changes.get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get(0).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get(0).getFieldOldValue().toString())
                        .isEqualTo("\"State2\"");
                assertThat(changes.get(0).getFieldNewValue().toString())
                        .isEqualTo("\"State4\"");

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
