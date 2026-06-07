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
}
