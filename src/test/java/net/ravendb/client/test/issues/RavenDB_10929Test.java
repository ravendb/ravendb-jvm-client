package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.exceptions.database.DatabaseDisabledException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.UpdateDatabaseOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisabledOnPullRequest
public class RavenDB_10929Test extends RemoteTestBase {

    @Test
    public void canUpdateDatabaseRecord() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            DatabaseRecordWithEtag record = store.maintenance().server()
                    .send(new GetDatabaseRecordOperation(store.getDatabase()));

            long etag = record.getEtag();
            assertThat(record)
                    .isNotNull();
            assertThat(etag)
                    .isPositive();
            assertThat(record.isDisabled())
                    .isFalse();

            record.setDisabled(true);

            store.maintenance().server().send(new UpdateDatabaseOperation(record, etag));

            record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record)
                    .isNotNull();
            assertThat(record.getEtag())
                    .isGreaterThan(etag);
            assertThat(record.isDisabled())
                    .isTrue();

            final DatabaseRecordWithEtag recordCopy = record;

            assertThatThrownBy(() -> store.maintenance().server().send(new CreateDatabaseOperation(recordCopy)))
                    .isExactlyInstanceOf(ConcurrencyException.class);

            assertThatThrownBy(() -> {
                try (IDocumentSession session = store.openSession()) {
                    session.store(new Company());
                }
            }).isInstanceOf(DatabaseDisabledException.class);

            assertThatThrownBy(() -> {
                try (IDocumentSession session = store.openSession()) {
                    session.store(new Company(), "id");
                    session.saveChanges();
                }
            }).isInstanceOf(DatabaseDisabledException.class);
        }
    }

    @Test
    public void canUpdateCompressionViaUpdateDatabaseRecord() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            DatabaseRecordWithEtag record = store.maintenance().server()
                    .send(new GetDatabaseRecordOperation(store.getDatabase()));

            long etag = record.getEtag();
            assertThat(record)
                    .isNotNull();
            assertThat(etag)
                    .isPositive();
            assertThat(record.isDisabled())
                    .isFalse();

            DocumentsCompressionConfiguration documentsCompressionConfiguration = new DocumentsCompressionConfiguration();
            documentsCompressionConfiguration.setCollections(new String[] { "Users" });
            documentsCompressionConfiguration.setCompressRevisions(true);
            record.setDocumentsCompression(documentsCompressionConfiguration);

            store.maintenance().server().send(new UpdateDatabaseOperation(record, etag));

            record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record)
                    .isNotNull();
            assertThat(record.getEtag())
                    .isGreaterThan(etag);
            assertThat(record.getDocumentsCompression().getCollections())
                    .isEqualTo(new String[] { "Users" });
            assertThat(record.getDocumentsCompression().isCompressRevisions())
                    .isTrue();
        }
    }
}
