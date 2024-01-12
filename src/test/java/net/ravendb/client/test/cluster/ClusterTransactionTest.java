package net.ravendb.client.test.cluster;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.GetCompareExchangeValueOperation;
import net.ravendb.client.documents.operations.compareExchange.PutCompareExchangeValueOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClusterTransactionTest extends RemoteTestBase {

    @Test
    public void canCreateClusterTransactionRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");

            User user3 = new User();
            user3.setName("Indych");

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            sessionOptions.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("usernames/ayende", user1);
                session.store(user3, "foo/bar");
                session.saveChanges();

                User user = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/ayende").getValue();
                assertThat(user.getName())
                        .isEqualTo(user1.getName());
                user = session.load(User.class, "foo/bar");
                assertThat(user.getName())
                        .isEqualTo(user3.getName());
            }
        }
    }

    @Test
    public void canDeleteCompareExchangeValue() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");

            User user3 = new User();
            user3.setName("Indych");

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            sessionOptions.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("usernames/ayende", user1);
                session.advanced().clusterTransaction().createCompareExchangeValue("usernames/marcin", user3);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<User> compareExchangeValue = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/ayende");
                assertThat(compareExchangeValue)
                        .isNotNull();
                session.advanced().clusterTransaction().deleteCompareExchangeValue(compareExchangeValue);

                CompareExchangeValue<User> compareExchangeValue2 = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/marcin");
                assertThat(compareExchangeValue2)
                        .isNotNull();
                session.advanced().clusterTransaction().deleteCompareExchangeValue("usernames/marcin", compareExchangeValue2.getIndex());

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<User> compareExchangeValue = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/ayende");
                CompareExchangeValue<User> compareExchangeValue2 = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/marcin");


                assertThat(compareExchangeValue)
                        .isNull();
                assertThat(compareExchangeValue2)
                        .isNull();
            }
        }
    }

    @Test
    public void testSessionSequance() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");

            User user2 = new User();
            user2.setName("Indych");

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            sessionOptions.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("usernames/ayende", user1);
                session.store(user1, "users/1");
                session.saveChanges();

                CompareExchangeValue<User> value = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/ayende");
                value.setValue(user2);

                session.store(user2, "users/2");
                user1.setAge(10);
                session.store(user1, "users/1");
                session.saveChanges();
            }
        }
    }

    @Test
    public void throwOnUnsupportedOperations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            sessionOptions.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {

                ByteArrayInputStream attachmentStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
                session.advanced().attachments().store("asd", "test", attachmentStream);
                assertThatThrownBy(session::saveChanges).isInstanceOfAny(IllegalStateException.class);
            }
        }
    }

    @Test
    public void throwOnInvalidTransactionMode() throws Exception {
        User user1 = new User();
        user1.setName("Karmel");

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.advanced().clusterTransaction().createCompareExchangeValue("usernames/ayende", user1)).isExactlyInstanceOf(IllegalStateException.class);

                assertThatThrownBy(() -> session.advanced().clusterTransaction().deleteCompareExchangeValue("usernames/ayende", 0)).isExactlyInstanceOf(IllegalStateException.class);
            }

            SessionOptions options = new SessionOptions();
            options.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            options.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

            try (IDocumentSession session = store.openSession(options)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("usernames/ayende", user1);
                session.advanced().setTransactionMode(TransactionMode.SINGLE_NODE);
                assertThatThrownBy(session::saveChanges).isExactlyInstanceOf(IllegalStateException.class);

                session.advanced().setTransactionMode(TransactionMode.CLUSTER_WIDE);
                session.saveChanges();

                CompareExchangeValue<User> u = session.advanced().clusterTransaction().getCompareExchangeValue(User.class, "usernames/ayende");
                assertThat(u.getValue().getName())
                        .isEqualTo(user1.getName());
            }
        }
    }

    @Test
    public void blockWorkingWithAtomicGuardBySession() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User doc = new User();
                doc.setName("Grisha");
                session.store(doc, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                assertThatThrownBy(() -> {
                    session.advanced().clusterTransaction().getCompareExchangeValue(String.class, getAtomicGuardKey("users/1"));
                }).hasMessageContaining("'rvn-atomic/users/1' is an atomic guard and you cannot load it via the session");
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue(getAtomicGuardKey("users/2"), "foo");
                assertThatThrownBy(session::saveChanges)
                        .hasMessageContaining("You cannot manipulate the atomic guard 'rvn-atomic/users/2' via the cluster-wide session");
            }


        }
    }

    private static String getAtomicGuardKey(String id) {
        return Constants.CompareExchange.RVN_ATOMIC_PREFIX + id;
    }

    public static class AtomicGuard {
        private String id;
        private boolean locked;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }
    }

    @Test
    public void canModifyingAtomicGuardViaOperations() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String docId = "users/1";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User doc = new User();
                doc.setName("Grisha");
                session.store(doc, docId);
                session.saveChanges();
            }

            String compareExchangeKey = "rvn-atomic/" + docId;
            CompareExchangeValue<AtomicGuard> val = store.operations().send(new GetCompareExchangeValueOperation<>(AtomicGuard.class, compareExchangeKey));
            val.getValue().locked = true;

            store.operations().send(new PutCompareExchangeValueOperation<>(val.getKey(), val.getValue(), val.getIndex()));

            val = store.operations().send(new GetCompareExchangeValueOperation<>(AtomicGuard.class, compareExchangeKey));
            assertThat(val.getValue().locked)
                    .isTrue();
        }
    }
}
