package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsBinCleanerOperation;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsBinCleanerOperationResult;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsBinConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_21326Test extends RemoteTestBase {

    @Test
    public void revisionsBinCleanerTest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RevisionsCollectionConfiguration defaultConfig = new RevisionsCollectionConfiguration();
            defaultConfig.setDisabled(false);
            defaultConfig.setMinimumRevisionsToKeep(100L);
            RevisionsConfiguration revisionsConfig = new RevisionsConfiguration();
            revisionsConfig.setDefaultConfig(defaultConfig);
            store.maintenance().send(new ConfigureRevisionsOperation(revisionsConfig));

            User user1 = new User();
            user1.setId("Users/1-A");
            user1.setName("Shahar");
            User user2 = new User();
            user2.setId("Users/2-B");
            user2.setName("Shahar");

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.store(user2);
                session.saveChanges();

                for (int i = 1; i <= 10; i++) {
                    session.load(User.class, user1.getId()).setName("Shahar" + i);
                    session.load(User.class, user2.getId()).setName("Shahar" + i);
                    session.saveChanges();
                }

                session.delete(user1.getId());
                session.delete(user2.getId());
                session.saveChanges();

                session.store(user2); // revive user2
                session.saveChanges();

                assertThat(session.advanced().revisions().getCountFor(user1.getId())).isEqualTo(12);
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(13);
            }

            RevisionsBinConfiguration config = new RevisionsBinConfiguration();
            config.setMinimumEntriesAgeToKeepInMin(0);
            config.setCleanerFrequencyInSec(1);
            configRevisionsBinCleaner(store, config);

            Long count = waitForValue(() -> {
                try (IDocumentSession session = store.openSession()) {
                    return session.advanced().revisions().getCountFor(user1.getId());
                }
            }, 0L);
            assertThat(count).isEqualTo(0L);

            // End Cleanup
            config.setDisabled(true);
            configRevisionsBinCleaner(store, config);

            try (IDocumentSession session = store.openSession()) {
                session.delete(user2.getId());
                session.saveChanges(); // delete user2
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().revisions().getCountFor(user2.getId())).isEqualTo(14);
            }
        }
    }

    @Test
    public void defaultsAndDatabaseRecordBuilder() {
        RevisionsBinConfiguration config = new RevisionsBinConfiguration();
        assertThat(config.isDisabled()).isFalse();
        assertThat(config.getMinimumEntriesAgeToKeepInMin()).isEqualTo(30 * 24 * 60);
        assertThat(config.getCleanerFrequencyInSec()).isEqualTo(5 * 60);

        DatabaseRecord record = DatabaseRecordBuilder.create()
                .regular("db")
                .configureRevisionsBin(config)
                .toDatabaseRecord();
        assertThat(record.getRevisionsBin()).isSameAs(config);

        assertThatThrownBy(() -> new ConfigureRevisionsBinCleanerOperation(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void configRevisionsBinCleaner(IDocumentStore store, RevisionsBinConfiguration config) {
        ConfigureRevisionsBinCleanerOperationResult result = store.maintenance().send(new ConfigureRevisionsBinCleanerOperation(config));
        assertThat(result.getRaftCommandIndex()).isNotNull();

        DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
        assertThat(record.getRevisionsBin()).isEqualTo(config);
    }
}
