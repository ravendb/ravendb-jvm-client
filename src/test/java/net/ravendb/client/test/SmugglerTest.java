package net.ravendb.client.test;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.smuggler.BackupUtils;
import net.ravendb.client.documents.smuggler.DatabaseItemType;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerExportOptions;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerImportOptions;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.test.client.QueryTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SmugglerTest extends RemoteTestBase {

    @Test
    public void canExportDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            File exportFile = File.createTempFile("exported-db-", "." + Constants.Documents.PeriodicBackup.FULL_BACKUP_EXTENSION);
            exportFile.deleteOnExit();

            DatabaseSmugglerExportOptions options = new DatabaseSmugglerExportOptions();
            Operation operation = store.smuggler().exportAsync(options, exportFile.getAbsolutePath());
            operation.waitForCompletion();

            assertThat(exportFile)
                    .exists();
            assertThat(exportFile.length())
                    .isNotZero();
        }
    }

    @Test
    public void canImportExportedDatabase() throws Exception {
        File exportFile = File.createTempFile("exported-db-", "." + Constants.Documents.PeriodicBackup.FULL_BACKUP_EXTENSION);
        exportFile.deleteOnExit();

        try (IDocumentStore store = getDocumentStore()) {
            addUsers(store);

            DatabaseSmugglerExportOptions options = new DatabaseSmugglerExportOptions();
            Operation operation = store.smuggler().exportAsync(options, exportFile.getAbsolutePath());
            operation.waitForCompletion();
        }

        try (IDocumentStore store = getDocumentStore()) {
            DatabaseSmugglerImportOptions options = new DatabaseSmugglerImportOptions();
            Operation operation = store.smuggler().importAsync(options, exportFile.getAbsolutePath());
            operation.waitForCompletion();

            DatabaseStatistics stats = store.maintenance().send(new GetStatisticsOperation());
            assertThat(stats.getCountOfIndexes())
                    .isEqualTo(1);
            assertThat(stats.getCountOfDocuments())
                    .isEqualTo(3);
            assertThat(stats.getCountOfTimeSeriesSegments())
                    .isEqualTo(1);
            assertThat(stats.getCountOfCounterEntries())
                    .isEqualTo(1);
        }
    }

    @Test
    public void canUseBetweenOption() throws Exception {
        try (IDocumentStore sourceStore = getDocumentStore()) {
            addUsers(sourceStore);

            try (IDocumentStore targetStore = getDocumentStore()) {

                DatabaseSmugglerExportOptions options = new DatabaseSmugglerExportOptions();
                options.setOperateOnTypes(EnumSet.of(DatabaseItemType.DOCUMENTS));
                Operation exportOperation = sourceStore.smuggler().exportAsync(options, targetStore.smuggler());

                exportOperation.waitForCompletion();

                DatabaseStatistics stats = targetStore.maintenance().send(new GetStatisticsOperation());
                assertThat(stats.getCountOfIndexes())
                        .isEqualTo(0); // we didn't request indexes to be copied
                assertThat(stats.getCountOfDocuments())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void canSortFiles() throws Exception {
        String[] files = new String[] {
                "2018-11-08-10-47.ravendb-incremental-backup",
                "2018-11-08-10-46.ravendb-incremental-backup",
                "2018-11-08-10-46.ravendb-full-backup"
        };

        List<File> sortedFiles = Arrays.stream(files)
                .map(x -> new File(x))
                .sorted(BackupUtils.COMPARATOR)
                .collect(Collectors.toList());

        assertThat(sortedFiles.get(0).getName())
                .isEqualTo("2018-11-08-10-46.ravendb-full-backup");
        assertThat(sortedFiles.get(1).getName())
                .isEqualTo("2018-11-08-10-46.ravendb-incremental-backup");
        assertThat(sortedFiles.get(2).getName())
                .isEqualTo("2018-11-08-10-47.ravendb-incremental-backup");
    }

    private void addUsers(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            User user1 = new User();
            user1.setName("John");
            user1.setAge(3);

            User user2 = new User();
            user2.setName("John");
            user2.setAge(5);

            User user3 = new User();
            user3.setName("Tarzan");
            user3.setAge(2);

            session.store(user1, "users/1");
            session.store(user2, "users/2");
            session.store(user3, "users/3");

            session.countersFor("users/1")
                    .increment("stars");

            session.timeSeriesFor("users/1", "Stars")
                    .append(new Date(), 5);

            session.saveChanges();
        }

        store.executeIndex(new QueryTest.UsersByName());
        waitForIndexing(store);
    }
}
