package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.indexes.IndexDoesNotExistException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_12790Test extends RemoteTestBase {

    @Test
    public void lazyQueryAgainstMissingIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Document document = new Document();
                document.setName("name");
                session.store(document);
                session.saveChanges();
            }

            // intentionally not creating the index that we query against

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.query(Document.class, DocumentIndex.class).toList())
                        .isExactlyInstanceOf(IndexDoesNotExistException.class);
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<List<Document>> lazyQuery = session.query(Document.class, DocumentIndex.class)
                        .lazily();

                assertThatThrownBy(() -> lazyQuery.getValue())
                        .isExactlyInstanceOf(IndexDoesNotExistException.class);
            }
        }
    }

    public static class DocumentIndex extends AbstractIndexCreationTask {
    }

    public static class Document {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
