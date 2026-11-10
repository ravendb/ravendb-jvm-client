package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.operations.transactionsRecording.ReplayTransactionsRecordingOperation;
import net.ravendb.client.documents.operations.transactionsRecording.ReplayTxOperationResult;
import net.ravendb.client.documents.operations.transactionsRecording.StartTransactionsRecordingOperation;
import net.ravendb.client.documents.operations.transactionsRecording.StopTransactionsRecordingOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class RecordingTransactionOperationsMergerTest extends RemoteTestBase {

    @Test
    @Disabled("waiting for RavenDB-15106")
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

    @Test
    public void canReplayRecordedTransactions() throws Exception {
        Path directory = Files.createTempDirectory("transactions-recording");
        File recordFile = directory.resolve("recording.log").toFile();

        try {
            try (IDocumentStore store = getDocumentStore()) {
                store.maintenance().send(new StartTransactionsRecordingOperation(recordFile.getAbsolutePath()));

                try {
                    try (IDocumentSession session = store.openSession()) {
                        User user1 = new User();
                        user1.setName("John");
                        session.store(user1, "users/1");

                        User user2 = new User();
                        user2.setName("Jane");
                        session.store(user2, "users/2");
                        session.saveChanges();
                    }

                    try (IDocumentSession session = store.openSession()) {
                        User user3 = new User();
                        user3.setName("Bob");
                        session.store(user3, "users/3");
                        session.delete("users/2");
                        session.saveChanges();
                    }
                } finally {
                    store.maintenance().send(new StopTransactionsRecordingOperation());
                }
            }

            assertThat(recordFile.length())
                    .isPositive();

            try (IDocumentStore store = getDocumentStore()) {
                GetNextOperationIdCommand command = new GetNextOperationIdCommand();
                store.getRequestExecutor().execute(command);

                ReplayTxOperationResult result;
                try (InputStream replayStream = new FileInputStream(recordFile)) {
                    result = store.maintenance().send(new ReplayTransactionsRecordingOperation(replayStream, command.getResult()));
                }

                assertThat(result.getExecutedCommandsAmount())
                        .isPositive();
                assertThat(result.getMessage())
                        .isEqualTo("Processed " + result.getExecutedCommandsAmount() + " commands.");

                try (IDocumentSession session = store.openSession()) {
                    assertThat(session.load(User.class, "users/1").getName())
                            .isEqualTo("John");
                    assertThat(session.load(User.class, "users/2"))
                            .isNull();
                    assertThat(session.load(User.class, "users/3").getName())
                            .isEqualTo("Bob");
                }
            }
        } finally {
            recordFile.delete();
            directory.toFile().delete();
        }
    }
}
