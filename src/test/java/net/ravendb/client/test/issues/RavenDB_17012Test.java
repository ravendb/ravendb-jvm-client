package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.bulkInsert.BulkInsertOptions;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17012Test extends RemoteTestBase {

    @Test
    public void can_SkipOverwriteIfUnchanged() throws Exception {
        int docsCount = 500;

        try (DocumentStore store = getDocumentStore()) {
            List<User> docs = new ArrayList<>();

            try (BulkInsertOperation bulk = store.bulkInsert()) {

                for (int i = 0; i < docsCount; i++) {
                    User user = new User();
                    docs.add(user);
                    bulk.store(user, String.valueOf(i));
                }
            }

            DatabaseStatistics stats = store.maintenance().send(new GetStatisticsOperation());
            Long lastEtag = stats.getLastDocEtag();


            BulkInsertOptions options = new BulkInsertOptions();
            options.setSkipOverwriteIfUnchanged(true);
            try (BulkInsertOperation bulk = store.bulkInsert(options)) {
                for (int i = 0; i < docsCount; i++) {
                    User doc = docs.get(i);
                    bulk.store(doc, String.valueOf(i));
                }
            }

            stats = store.maintenance().send(new GetStatisticsOperation());
            assertThat(stats.getLastDocEtag())
                    .isEqualTo(lastEtag);
        }
    }

    @Test
    public void can_SkipOverwriteIfUnchanged_SomeDocuments() throws Exception {
        int docsCount = 500;

        try (DocumentStore store = getDocumentStore()) {
            List<User> docs = new ArrayList<>();

            try (BulkInsertOperation bulk = store.bulkInsert()) {

                for (int i = 0; i < docsCount; i++) {
                    User user = new User();
                    user.setAge(i);
                    docs.add(user);
                    bulk.store(user, String.valueOf(i));
                }
            }

            DatabaseStatistics stats = store.maintenance().send(new GetStatisticsOperation());
            Long lastEtag = stats.getLastDocEtag();


            BulkInsertOptions options = new BulkInsertOptions();
            options.setSkipOverwriteIfUnchanged(true);
            try (BulkInsertOperation bulk = store.bulkInsert(options)) {
                for (int i = 0; i < docsCount; i++) {
                    User doc = docs.get(i);
                    if (i % 2 == 0) {
                        doc.setAge(2 * (i + 1));
                    }
                    bulk.store(doc, String.valueOf(i));
                }
            }

            stats = store.maintenance().send(new GetStatisticsOperation());
            assertThat(stats.getLastDocEtag())
                    .isEqualTo(lastEtag + docsCount / 2);
        }
    }
}
