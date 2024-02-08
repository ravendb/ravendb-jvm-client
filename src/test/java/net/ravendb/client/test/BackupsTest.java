package net.ravendb.client.test;

import com.google.common.base.Stopwatch;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.backups.*;
import net.ravendb.client.documents.operations.backups.sharding.GetShardedPeriodicBackupStatusOperation;
import net.ravendb.client.documents.operations.ongoingTasks.NextBackup;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTask;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskBackup;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class BackupsTest extends RemoteTestBase {

    @Test
    public void canBackupDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Path backup = Files.createTempDirectory("backup");

            try {
                UpdatePeriodicBackupOperationResult backupOperationResult = configureBackup(BackupType.SNAPSHOT, backup, store);

                waitForResponsibleNodeUpdate(store, backupOperationResult.getTaskId());

                StartBackupOperation startBackupOperation = new StartBackupOperation(true, backupOperationResult.getTaskId());
                StartBackupOperationResult send = store.maintenance().send(startBackupOperation);
                long backupOperation = send.getOperationId();
                assertThat(backupOperation)
                        .isPositive();

                waitForBackup(backup);
                waitForBackupStatus(store, backupOperationResult.getTaskId(), false);

                OngoingTaskBackup myBackup = (OngoingTaskBackup) store.maintenance().send(new GetOngoingTaskInfoOperation("myBackup", OngoingTaskType.BACKUP));

                assertThat(myBackup)
                        .isNotNull();
                NextBackup nextBackup = myBackup.getNextBackup();
                assertThat(nextBackup)
                        .isNotNull();
                assertThat(nextBackup.getDateTime())
                        .isInTheFuture();
                assertThat(nextBackup.getTimeSpan())
                        .isGreaterThan(Duration.ZERO);
            } finally {
                backup.toAbsolutePath().toFile().deleteOnExit();

                // make sure backup was finished
                Thread.sleep(500);
                backup.toAbsolutePath().toFile().delete();
            }
        }
    }

    private static UpdatePeriodicBackupOperationResult configureBackup(BackupType snapshot, Path backup, IDocumentStore store) {
        PeriodicBackupConfiguration backupConfiguration = new PeriodicBackupConfiguration();
        backupConfiguration.setName("myBackup");
        backupConfiguration.setBackupType(snapshot);
        backupConfiguration.setFullBackupFrequency("20 * * * *");

        LocalSettings localSettings = new LocalSettings();
        localSettings.setFolderPath(backup.toAbsolutePath().toString());

        backupConfiguration.setLocalSettings(localSettings);
        UpdatePeriodicBackupOperation operation = new UpdatePeriodicBackupOperation(backupConfiguration);
        UpdatePeriodicBackupOperationResult backupOperationResult = store.maintenance().send(operation);
        return backupOperationResult;
    }

    @Test
    public void canBackupShardedDatabase() throws Exception {
        Consumer<DatabaseRecord> customize = record -> {
            DatabaseRecord databaseRecord = DatabaseRecordBuilder.create().sharded("test_db", b -> {
                b.addShard(1, s -> s.addNode("A"))
                        .addShard(2, s -> s.addNode("A"))
                        .orchestrator(o -> o.addNode("A"));
            }).toDatabaseRecord();

            record.setSharding(databaseRecord.getSharding());
        };

        try (IDocumentStore store = getDocumentStore("test_db", false, null, customize)) {
            Path backup = Files.createTempDirectory("backup");

            try {
                UpdatePeriodicBackupOperationResult backupOperationResult = configureBackup(BackupType.BACKUP, backup, store);

                waitForResponsibleNodeUpdate(store, backupOperationResult.getTaskId());

                StartBackupOperation startBackupOperation = new StartBackupOperation(true, backupOperationResult.getTaskId());
                StartBackupOperationResult send = store.maintenance().send(startBackupOperation);
                long backupOperation = send.getOperationId();
                assertThat(backupOperation)
                        .isPositive();

                waitForBackup(backup);
                waitForBackupStatus(store, backupOperationResult.getTaskId(), true);

                GetShardedPeriodicBackupStatusOperation.GetShardedPeriodicBackupStatusOperationResult backupResult =
                        store.maintenance().send(new GetShardedPeriodicBackupStatusOperation(backupOperationResult.getTaskId()));
                assertThat(backupResult)
                        .isNotNull();

                // props are asserted in waitForBackup method

            } finally {
                backup.toAbsolutePath().toFile().deleteOnExit();

                // make sure backup was finished
                Thread.sleep(500);
                backup.toAbsolutePath().toFile().delete();
            }
        }
    }

    @Test
    public void canSetupRetentionPolicy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            PeriodicBackupConfiguration backupConfiguration = new PeriodicBackupConfiguration();
            backupConfiguration.setName("myBackup");
            backupConfiguration.setDisabled(true);
            backupConfiguration.setBackupType(BackupType.SNAPSHOT);
            backupConfiguration.setFullBackupFrequency("20 * * * *");
            BackupEncryptionSettings encryptionSettings = new BackupEncryptionSettings();

            encryptionSettings.setEncryptionMode(EncryptionMode.USE_PROVIDED_KEY);
            encryptionSettings.setKey("QV2jJkHCPGwjbOiXuZDCNmyyj/GE4OH8OZlkg5jQPRI=");

            backupConfiguration.setBackupEncryptionSettings(encryptionSettings);

            RetentionPolicy retentionPolicy = new RetentionPolicy();
            retentionPolicy.setDisabled(false);
            retentionPolicy.setMinimumBackupAgeToKeep(Duration.ofDays(1).plusHours(3).plusMinutes(2));

            backupConfiguration.setRetentionPolicy(retentionPolicy);

            UpdatePeriodicBackupOperation operation = new UpdatePeriodicBackupOperation(backupConfiguration);
            store.maintenance().send(operation);

            OngoingTaskBackup myBackup = (OngoingTaskBackup) store.maintenance().send(new GetOngoingTaskInfoOperation("myBackup", OngoingTaskType.BACKUP));

            assertThat(myBackup.getRetentionPolicy().getMinimumBackupAgeToKeep().toString())
                    .isEqualTo("PT27H2M");
            assertThat(myBackup.isEncrypted())
                    .isTrue();
        }
    }

    private void waitForResponsibleNodeUpdate(IDocumentStore store, long taskId) throws InterruptedException {
        waitForValue(() -> {
            OngoingTask task = store.maintenance().send(new GetOngoingTaskInfoOperation(taskId, OngoingTaskType.BACKUP));
            return task.getResponsibleNode() != null && task.getResponsibleNode().getNodeTag() != null;
        }, true);
    }

    private void waitForBackup(Path backup) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.MILLISECONDS) < 30_000) {

            if (Files.list(backup).count() > 0) {
                return;
            }

            Thread.sleep(200);
        }

        throw new IllegalStateException("Unable to find backup files in: " + backup.toAbsolutePath());
    }

    private void waitForBackupStatus(IDocumentStore store, long taskId, boolean sharded) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();

        while (sw.elapsed(TimeUnit.MILLISECONDS) < 10_000) {
            if (sharded) {
                GetShardedPeriodicBackupStatusOperation.GetShardedPeriodicBackupStatusOperationResult backupStatus = store.maintenance()
                        .send(new GetShardedPeriodicBackupStatusOperation(taskId));

                if (backupStatus != null) {
                    if (backupStatus.getStatuses().values().stream().allMatch(x -> x != null && x.getLastFullBackup() != null)) {
                        return;
                    }
                }
            }else {
                GetPeriodicBackupStatusOperationResult backupStatus = store.maintenance()
                        .send(new GetPeriodicBackupStatusOperation(taskId));

                if (backupStatus != null
                        && backupStatus.getStatus() != null
                        && backupStatus.getStatus().getLastFullBackup() != null) {
                    return;
                }
            }


            Thread.sleep(200);
        }

        throw new IllegalStateException("Unable to get expected backup status");
    }
}
