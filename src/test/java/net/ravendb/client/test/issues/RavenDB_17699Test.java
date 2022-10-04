package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.ConditionalLoadResult;
import net.ravendb.client.documents.session.IDocumentSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17699Test extends RemoteTestBase {
    @Test
    public void multipleConditionalGetQueries() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Item book = new Item();
                book.setName("book");
                session.store(book, "items/book");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<ConditionalLoadResult<Item>> bookLazy =
                        session.advanced().lazily().conditionalLoad(Item.class, "items/book", "bad-value");
                session.advanced().eagerly().executeAllPendingLazyOperations();
                assertThat(bookLazy.getValue().getEntity())
                        .isNotNull();
            }
            try (IDocumentSession session = store.openSession()) {
                Lazy<ConditionalLoadResult<Item>> bookLazy =
                        session.advanced().lazily().conditionalLoad(Item.class, "items/book", "bad-value");
                session.advanced().eagerly().executeAllPendingLazyOperations();
                assertThat(bookLazy.getValue().getEntity())
                        .isNotNull();
            }
        }
    }

    public static class Item {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
