package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.backups.BackupType;
import net.ravendb.client.documents.operations.backups.BackupUploadMode;
import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.documents.operations.backups.S3Settings;
import net.ravendb.client.documents.operations.backups.S3StorageClass;
import net.ravendb.client.documents.operations.backups.UpdatePeriodicBackupOperation;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskBackup;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_22732Test extends RemoteTestBase {

    @Test
    public void ongoingTaskBackupContainsFrequenciesAndUploadMode() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            // the task is disabled, so the (fake) S3 destination is never contacted
            S3Settings s3Settings = new S3Settings();
            s3Settings.setBucketName("ravendb-test-bucket");
            s3Settings.setAwsRegionName("us-east-1");
            s3Settings.setAwsAccessKey("access-key");
            s3Settings.setAwsSecretKey("secret-key");
            s3Settings.setStorageClass(S3StorageClass.DEEP_ARCHIVE); // RavenDB-23846

            PeriodicBackupConfiguration configuration = new PeriodicBackupConfiguration();
            configuration.setName("myBackup");
            configuration.setDisabled(true);
            configuration.setBackupType(BackupType.BACKUP);
            configuration.setFullBackupFrequency("0 2 * * 0");
            configuration.setIncrementalBackupFrequency("0 2 * * 1");
            configuration.setBackupUploadMode(BackupUploadMode.DIRECT_UPLOAD);
            configuration.setS3Settings(s3Settings);
            configuration.setMaxReadOpsPerSecond(1024); // RavenDB-23034

            store.maintenance().send(new UpdatePeriodicBackupOperation(configuration));

            OngoingTaskBackup backup = (OngoingTaskBackup) store.maintenance()
                    .send(new GetOngoingTaskInfoOperation("myBackup", OngoingTaskType.BACKUP));

            assertThat(backup.getFullBackupFrequency())
                    .isEqualTo("0 2 * * 0");
            assertThat(backup.getIncrementalBackupFrequency())
                    .isEqualTo("0 2 * * 1");
            assertThat(backup.getBackupUploadMode())
                    .isEqualTo(BackupUploadMode.DIRECT_UPLOAD);
            assertThat(backup.isHasCloudBackup())
                    .isTrue();

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record.getPeriodicBackups())
                    .hasSize(1);
            PeriodicBackupConfiguration storedConfiguration = record.getPeriodicBackups().get(0);
            assertThat(storedConfiguration.getMaxReadOpsPerSecond())
                    .isEqualTo(1024);
            assertThat(storedConfiguration.getS3Settings().getStorageClass())
                    .isEqualTo(S3StorageClass.DEEP_ARCHIVE);
        }
    }
}
