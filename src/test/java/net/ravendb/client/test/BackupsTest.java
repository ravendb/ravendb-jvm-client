package net.ravendb.client.test;

import com.google.common.base.Stopwatch;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.backups.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConflictException;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class BackupsTest extends RemoteTestBase {

    @Test
    public void canBackupDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Path backup = Files.createTempDirectory("backup");

            try {
                PeriodicBackupConfiguration backupConfiguration = new PeriodicBackupConfiguration();
                backupConfiguration.setName("myBackup");
                backupConfiguration.setBackupType(BackupType.SNAPSHOT);
                backupConfiguration.setFullBackupFrequency("20 * * * *");

                LocalSettings localSettings = new LocalSettings();
                localSettings.setFolderPath(backup.toAbsolutePath().toString());

                backupConfiguration.setLocalSettings(localSettings);
                UpdatePeriodicBackupOperation operation = new UpdatePeriodicBackupOperation(backupConfiguration);
                UpdatePeriodicBackupOperationResult backupOperationResult = store.maintenance().send(operation);

                StartBackupOperation startBackupOperation = new StartBackupOperation(true, backupOperationResult.getTaskId());
                StartBackupOperationResult send = store.maintenance().send(startBackupOperation);
                int backupOperation = send.getOperationId();

                waitForBackup(backup);

                GetPeriodicBackupStatusOperationResult backupStatus = store.maintenance()
                        .send(new GetPeriodicBackupStatusOperation(backupOperationResult.getTaskId()));

                assertThat(backupStatus)
                        .isNotNull();

                assertThat(backupStatus.getStatus().getLastFullBackup())
                        .isNotNull();
            } finally {
                assertThat(backup.toAbsolutePath().toFile().delete())
                        .isTrue();
            }
        }

    }

    private void waitForBackup(Path backup) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.MILLISECONDS) < 10_000) {

            if (Files.list(backup).count() > 0) {
                return;
            }

            Thread.sleep(200);
        }
    }
}
