package net.ravendb.client.test.client.bulkInsert;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.bulkinsert.BulkInsertAbortedException;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BulkInsertsTest extends RemoteTestBase {

    @Test
    public void simpleBulkInsertShouldWork() throws Exception {

        FooBar fooBar1 = new FooBar();
        fooBar1.setName("John Doe");

        FooBar fooBar2 = new FooBar();
        fooBar2.setName("Jane Doe");

        FooBar fooBar3 = new FooBar();
        fooBar3.setName("Mega John");

        FooBar fooBar4 = new FooBar();
        fooBar4.setName("Mega Jane");

        try (IDocumentStore store = getDocumentStore()) {

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                bulkInsert.store(fooBar1);
                bulkInsert.store(fooBar2);
                bulkInsert.store(fooBar3);
                bulkInsert.store(fooBar4);
            }

            try (IDocumentSession session = store.openSession()) {
                FooBar doc1 = session.load(FooBar.class, "FooBars/1-A");
                FooBar doc2 = session.load(FooBar.class, "FooBars/2-A");
                FooBar doc3 = session.load(FooBar.class, "FooBars/3-A");
                FooBar doc4 = session.load(FooBar.class, "FooBars/4-A");

                assertThat(doc1)
                        .isNotNull();
                assertThat(doc2)
                        .isNotNull();
                assertThat(doc3)
                        .isNotNull();
                assertThat(doc4)
                        .isNotNull();

                assertThat(doc1.getName())
                        .isEqualTo("John Doe");
                assertThat(doc2.getName())
                        .isEqualTo("Jane Doe");
                assertThat(doc3.getName())
                        .isEqualTo("Mega John");
                assertThat(doc4.getName())
                        .isEqualTo("Mega Jane");

            }
        }

    }

    @Test
    public void killedToEarly() {
        assertThatThrownBy(() -> {
            try (IDocumentStore store = getDocumentStore()) {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    bulkInsert.store(new FooBar());
                    bulkInsert.abort();
                    bulkInsert.store(new FooBar());
                }
            }
        }).isExactlyInstanceOf(BulkInsertAbortedException.class);
    }

    @Test
    public void shouldNotAcceptIdsEndingWithPipeLine() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                assertThatThrownBy(() -> bulkInsert.store(new FooBar(), "foobars|")).isExactlyInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Document ids cannot end with '|', but was called with foobars|");
            }
        }
    }

    @Test
    public void canModifyMetadataWithBulkInsert() throws Exception {
        String expirationDate = NetISO8601Utils.format(DateUtils.addYears(new Date(), 1), true);

        try (IDocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                FooBar fooBar = new FooBar();
                fooBar.setName("Jon Show");
                MetadataAsDictionary metadata = new MetadataAsDictionary();
                metadata.put(Constants.Documents.Metadata.EXPIRES, expirationDate);

                bulkInsert.store(fooBar, metadata);
            }

            try (IDocumentSession session = store.openSession()) {
                FooBar entity = session.load(FooBar.class, "FooBars/1-A");
                Object metadataExpirationDate = session.advanced().getMetadataFor(entity).get(Constants.Documents.Metadata.EXPIRES);
                assertThat(metadataExpirationDate)
                        .isEqualTo(expirationDate);
            }
        }
    }


    public static class FooBar {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
