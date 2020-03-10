package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.transactionsRecording.StartTransactionsRecordingOperation;
import net.ravendb.client.documents.operations.transactionsRecording.StopTransactionsRecordingOperation;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class RecordingTransactionOperationsMergerTest extends RemoteTestBase {

    @Test
    public void canRecordTransactions() throws Exception {
        File targetFile = File.createTempFile("transactions", "log");
        targetFile.deleteOnExit();

        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new StartTransactionsRecordingOperation(targetFile.getAbsolutePath()));

            try {
                store.maintenance().send(new CreateSampleDataOperation());
            } finally {
                store.maintenance().send(new StopTransactionsRecordingOperation());
            }

            assertThat(targetFile.length())
                    .isPositive();
        }
    }
}
