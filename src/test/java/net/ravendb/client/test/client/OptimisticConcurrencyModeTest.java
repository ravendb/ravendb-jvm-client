package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OptimisticConcurrencyMode;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OptimisticConcurrencyModeTest extends RemoteTestBase {

    @Test
    public void conventionsRejectMixingModeWithDeprecatedFlag() {
        DocumentConventions conventions = new DocumentConventions();
        conventions.setUseOptimisticConcurrency(true);

        assertThat(conventions.getOptimisticConcurrencyMode())
                .isEqualTo(OptimisticConcurrencyMode.WRITES);
        assertThat(conventions.isUseOptimisticConcurrency())
                .isTrue();

        assertThatThrownBy(() -> conventions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("optimisticConcurrencyMode cannot be set when useOptimisticConcurrency was set");

        DocumentConventions otherConventions = new DocumentConventions();
        otherConventions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES);

        assertThatThrownBy(() -> otherConventions.setUseOptimisticConcurrency(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("useOptimisticConcurrency cannot be set when optimisticConcurrencyMode was set");
    }

    @Test
    public void sessionOptionsRejectIncompatibleCombinations() {
        SessionOptions noTrackingOptions = new SessionOptions();
        noTrackingOptions.setNoTracking(true);

        assertThatThrownBy(() -> noTrackingOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("noTracking is true");

        SessionOptions clusterWideOptions = new SessionOptions();
        clusterWideOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

        assertThatThrownBy(() -> clusterWideOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transactionMode");

        SessionOptions modeOptions = new SessionOptions();
        modeOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS);

        assertThatThrownBy(() -> modeOptions.setNoTracking(true))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> modeOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void sessionRejectsMixingModeWithDeprecatedFlag() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.advanced().setUseOptimisticConcurrency(true);

                assertThat(session.advanced().getOptimisticConcurrencyMode())
                        .isEqualTo(OptimisticConcurrencyMode.WRITES);

                assertThatThrownBy(() -> session.advanced().setOptimisticConcurrencyMode(OptimisticConcurrencyMode.NONE))
                        .isInstanceOf(IllegalStateException.class);
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES);

                assertThatThrownBy(() -> session.advanced().setUseOptimisticConcurrency(false))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void writesModeThrowsOnConcurrentModification() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("origin");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES);

                User user = session.load(User.class, "users/1");
                user.setName("first");

                try (IDocumentSession concurrentSession = store.openSession()) {
                    User concurrentUser = concurrentSession.load(User.class, "users/1");
                    concurrentUser.setName("second");
                    concurrentSession.saveChanges();
                }

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void writesAndReadsModeThrowsWhenAnyTrackedEntityWasModified() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("first");
                session.store(user1, "users/1");

                User user2 = new User();
                user2.setName("second");
                session.store(user2, "users/2");

                session.saveChanges();
            }

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                // both entities become tracked
                session.load(User.class, "users/1");
                User user2 = session.load(User.class, "users/2");

                // someone else modifies users/1 behind this session's back
                try (IDocumentSession concurrentSession = store.openSession()) {
                    User concurrentUser = concurrentSession.load(User.class, "users/1");
                    concurrentUser.setName("changed");
                    concurrentSession.saveChanges();
                }

                // only users/2 is modified here, but users/1 is stale - save must fail
                user2.setName("updated");

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void writesAndReadsModeSucceedsWhenNoTrackedEntityWasModified() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("first");
                session.store(user, "users/1");
                session.saveChanges();
            }

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User user = session.load(User.class, "users/1");
                user.setName("updated");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(User.class, "users/1").getName())
                        .isEqualTo("updated");
            }
        }
    }

    private static final String WATCHED = "docs/watched";
    private static final String OTHER = "docs/other";
    private static final String UNKNOWN_CHANGE_VECTOR = "A:1-aaaaaaaaaaaaaaaaaaaaaa";

    private static void seed(IDocumentStore store, String... idsAndNames) {
        try (IDocumentSession session = store.openSession()) {
            for (int i = 0; i < idsAndNames.length; i += 2) {
                User user = new User();
                user.setName(idsAndNames[i + 1]);
                session.store(user, idsAndNames[i]);
            }
            session.saveChanges();
        }
    }

    private static String changeVectorOf(IDocumentStore store, String id) {
        try (IDocumentSession session = store.openSession()) {
            return session.advanced().getChangeVectorFor(session.load(User.class, id));
        }
    }

    private static void rename(IDocumentStore store, String id, String name) {
        try (IDocumentSession session = store.openSession()) {
            session.load(User.class, id).setName(name);
            session.saveChanges();
        }
    }

    private static String nameOf(IDocumentStore store, String id) {
        try (IDocumentSession session = store.openSession()) {
            return session.load(User.class, id).getName();
        }
    }

    private static SessionOptions withMode(OptimisticConcurrencyMode mode) {
        SessionOptions options = new SessionOptions();
        options.setOptimisticConcurrencyMode(mode);
        return options;
    }

    @Test
    public void registerForConcurrencyCheck_WithEmptyId_ShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            assertThatThrownBy(() -> session.advanced().registerForConcurrencyCheck(null, "cv"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> session.advanced().registerForConcurrencyCheck("", "cv"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_ThrowsWhenRegisteredDocumentWasModified() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            String changeVector = changeVectorOf(store, WATCHED);
            rename(store, WATCHED, "Changed");

            // this session uses the default NONE mode, proving the registered check is honored regardless of mode
            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, OTHER).setName("Edited");
                session.advanced().registerForConcurrencyCheck(WATCHED, changeVector);

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOfSatisfying(ConcurrencyException.class, e -> assertThat(e.getId()).isEqualTo(WATCHED));
            }

            // the whole batch must have been rolled back, so docs/other is untouched
            assertThat(nameOf(store, OTHER)).isEqualTo("Other");
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_SucceedsWhenRegisteredDocumentUnchanged() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            String changeVector = changeVectorOf(store, WATCHED);

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, OTHER).setName("Edited");
                session.advanced().registerForConcurrencyCheck(WATCHED, changeVector);
                session.saveChanges();
            }

            assertThat(nameOf(store, OTHER)).isEqualTo("Edited");
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_EmptyChangeVectorAssertsDocumentDoesNotExist() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, OTHER, "Other");

            // the document we assert absent is created by another session
            seed(store, WATCHED, "Now exists");

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, OTHER).setName("Edited");
                session.advanced().registerForConcurrencyCheck(WATCHED, "");

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_EmptyChangeVectorSucceedsWhenDocumentStillAbsent() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, OTHER, "Other");

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, OTHER).setName("Edited");
                session.advanced().registerForConcurrencyCheck("docs/missing", "");
                session.saveChanges();
            }

            assertThat(nameOf(store, OTHER)).isEqualTo("Edited");
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_NullChangeVectorDisablesCheckForId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            try (IDocumentSession session = store.openSession(withMode(OptimisticConcurrencyMode.WRITES_AND_READS))) {
                // under WRITES_AND_READS this load is auto-tracked and would normally trigger a concurrency check
                session.load(User.class, WATCHED);
                session.load(User.class, OTHER).setName("Edited");

                rename(store, WATCHED, "Changed");

                // explicitly disable the check for the watched document
                session.advanced().registerForConcurrencyCheck(WATCHED, null);

                session.saveChanges();
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_IsConsumedByASuccessfulSaveChanges_AfterModify() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            try (IDocumentSession session = store.openSession()) {
                User watched = session.load(User.class, WATCHED);
                session.advanced().registerForConcurrencyCheck(WATCHED, session.advanced().getChangeVectorFor(watched));

                watched.setName("Changed");
                session.saveChanges(); // the check passes, and this very session advances the change vector

                session.load(User.class, OTHER).setName("Edited");
                session.saveChanges(); // must not re-assert the change vector this session already replaced
            }

            assertThat(nameOf(store, WATCHED)).isEqualTo("Changed");
            assertThat(nameOf(store, OTHER)).isEqualTo("Edited");
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_IsConsumedByASuccessfulSaveChanges_AfterDelete() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            try (IDocumentSession session = store.openSession()) {
                User watched = session.load(User.class, WATCHED);
                session.advanced().registerForConcurrencyCheck(WATCHED, session.advanced().getChangeVectorFor(watched));

                session.delete(watched);
                session.saveChanges(); // the check passes, and this very session deletes the document

                session.load(User.class, OTHER).setName("Edited");
                session.saveChanges(); // must not assert the old change vector against this session's own tombstone
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_IsKeptWhenSaveChangesFails() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            String staleChangeVector = changeVectorOf(store, WATCHED);
            rename(store, WATCHED, "Changed");

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, OTHER).setName("Edited");
                session.advanced().registerForConcurrencyCheck(WATCHED, staleChangeVector);

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);

                // a failed save must not consume the registration, so a retry fails the same way
                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_ExplicitChangeVector_IsNotOverwrittenByALaterLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            String staleChangeVector = changeVectorOf(store, WATCHED);
            rename(store, WATCHED, "Changed");

            try (IDocumentSession session = store.openSession(withMode(OptimisticConcurrencyMode.WRITES_AND_READS))) {
                session.advanced().registerForConcurrencyCheck(WATCHED, staleChangeVector);

                // loading the watched document must not replace the explicitly registered change vector
                session.load(User.class, WATCHED);

                session.load(User.class, OTHER).setName("Edited");

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOfSatisfying(ConcurrencyException.class, e -> assertThat(e.getId()).isEqualTo(WATCHED));
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_DisabledCheck_SurvivesALaterLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            try (IDocumentSession session = store.openSession(withMode(OptimisticConcurrencyMode.WRITES_AND_READS))) {
                // disable the check first, then load - the outcome must not depend on this order
                session.advanced().registerForConcurrencyCheck(WATCHED, null);
                session.load(User.class, WATCHED);

                rename(store, WATCHED, "Changed");

                session.load(User.class, OTHER).setName("Edited");

                session.saveChanges();
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_IsHonored_WhenDocumentIsAlsoWrittenWithItsOwnConcurrencyCheck() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original");

            String staleChangeVector = changeVectorOf(store, WATCHED);
            rename(store, WATCHED, "Changed");

            try (IDocumentSession session = store.openSession(withMode(OptimisticConcurrencyMode.WRITES))) {
                User watched = session.load(User.class, WATCHED); // tracked with the current change vector
                session.advanced().registerForConcurrencyCheck(WATCHED, staleChangeVector);

                watched.setName("Edited");

                // the write carries its own check, which uses the current change vector and would pass,
                // so only the explicitly registered (stale) check can fail this save
                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_LastRegistrationForAnIdWins() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            try (IDocumentSession session = store.openSession()) {
                session.advanced().registerForConcurrencyCheck(WATCHED, UNKNOWN_CHANGE_VECTOR);
                session.advanced().registerForConcurrencyCheck(WATCHED, null); // cancels the check

                session.load(User.class, OTHER).setName("Edited");
                session.saveChanges();
            }

            assertThat(nameOf(store, OTHER)).isEqualTo("Edited");
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_IsClearedByAdvancedClear() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original", OTHER, "Other");

            String staleChangeVector = changeVectorOf(store, WATCHED);
            rename(store, WATCHED, "Changed");

            try (IDocumentSession session = store.openSession()) {
                session.advanced().registerForConcurrencyCheck(WATCHED, staleChangeVector);
                session.advanced().clear();

                session.load(User.class, OTHER).setName("Edited");
                session.saveChanges();
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void registerForConcurrencyCheck_OnItsOwn_SendsExactlyOneRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seed(store, WATCHED, "Original");

            String changeVector = changeVectorOf(store, WATCHED);

            try (IDocumentSession session = store.openSession()) {
                session.advanced().registerForConcurrencyCheck(WATCHED, changeVector);
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests()).isEqualTo(1);
            }
        }
    }

    @Test
    public void registerForConcurrencyCheck_DisabledOnly_SendsNoRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            session.advanced().registerForConcurrencyCheck(WATCHED, UNKNOWN_CHANGE_VECTOR);
            session.advanced().registerForConcurrencyCheck(WATCHED, null);

            session.saveChanges();

            assertThat(session.advanced().getNumberOfRequests()).isZero();
        }
    }

    @Test
    public void registerForConcurrencyCheck_IsNotSupportedInANoTrackingSession() throws Exception {
        // a noTracking session rejects saveChanges as soon as the batch holds any command, so a registered
        // check can never reach the server from such a session
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions options = new SessionOptions();
            options.setNoTracking(true);

            try (IDocumentSession session = store.openSession(options)) {
                session.advanced().registerForConcurrencyCheck(WATCHED, UNKNOWN_CHANGE_VECTOR);

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void registerForConcurrencyCheck_IsNotSupportedInAClusterWideSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions options = new SessionOptions();
            options.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(options)) {
                session.advanced().registerForConcurrencyCheck(WATCHED, UNKNOWN_CHANGE_VECTOR);

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("registerForConcurrencyCheck");
            }
        }
    }
}
