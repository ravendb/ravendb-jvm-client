package net.ravendb.client.test.server.documents;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.documentsCompression.UpdateDocumentsCompressionConfigurationOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class CompressAllCollectionsTest extends RemoteTestBase {

    @Test
    public void compressAllCollectionsAfterDocsChange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            // we are running in memory - just check if command will be send to server
            store.maintenance().send(
                    new UpdateDocumentsCompressionConfigurationOperation(
                            new DocumentsCompressionConfiguration(false, true)));

            DatabaseRecord record = store.maintenance().server().send(
                    new GetDatabaseRecordOperation(store.getDatabase()));

            DocumentsCompressionConfiguration documentsCompression = record.getDocumentsCompression();

            assertThat(documentsCompression.isCompressAllCollections())
                    .isTrue();
            assertThat(documentsCompression.isCompressRevisions())
                    .isFalse();
        }
    }
}
