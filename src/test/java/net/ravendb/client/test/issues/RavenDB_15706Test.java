package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_15706Test extends RemoteTestBase {

    @Test
    public void bulkInsertWithoutDB() throws Exception {
        try (IDocumentStore outerStore = getDocumentStore()) {
            try (DocumentStore store = new DocumentStore()) {
                store.setUrls(outerStore.getUrls());
                store.initialize();

                assertThatThrownBy(store::bulkInsert)
                        .isExactlyInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
