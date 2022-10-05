package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15143Test extends RemoteTestBase {

    public static class Locker {
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class Command {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Test
    public void canUseCreateCmpXngToInSessionWithNoOtherChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.store(new Command(), "cmd/239-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                Command result = session.query(Command.class)
                        .include(i -> i.includeCompareExchangeValue("id"))
                        .first();

                assertThat(result)
                        .isNotNull();

                CompareExchangeValue<Locker> locker = session.advanced()
                        .clusterTransaction().getCompareExchangeValue(Locker.class, "cmd/239-A");
                assertThat(locker)
                        .isNull();

                Locker lockerObject = new Locker();
                lockerObject.setClientId("a");
                locker = session.advanced().clusterTransaction().createCompareExchangeValue("cmd/239-A", lockerObject);

                locker.getMetadata().put("@expires", DateUtils.addMinutes(new Date(), 2));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<String> smile = session.advanced().clusterTransaction().getCompareExchangeValue(String.class, "cmd/239-A");
                assertThat(smile)
                        .isNotNull();
            }
        }
    }

    @Test
    public void canStoreAndReadPrimitiveCompareExchange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/int", 5);
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/string", "testing");
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/true", true);
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/false", false);
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/zero", 0);
                session.advanced().clusterTransaction().createCompareExchangeValue("cmd/null", null);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<Integer> numberValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(Integer.class, "cmd/int");
                assertThat(numberValue.getValue())
                        .isEqualTo(5);

                CompareExchangeValue<String> stringValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(String.class, "cmd/string");
                assertThat(stringValue.getValue())
                        .isEqualTo("testing");

                CompareExchangeValue<Boolean> trueValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(Boolean.class, "cmd/true");
                assertThat(trueValue.getValue())
                        .isEqualTo(true);

                CompareExchangeValue<Boolean> falseValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(Boolean.class, "cmd/false");
                assertThat(falseValue.getValue())
                        .isEqualTo(false);

                CompareExchangeValue<Integer> zeroValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(Integer.class, "cmd/zero");
                assertThat(zeroValue.getValue())
                        .isEqualTo(0);

                CompareExchangeValue<Integer> nullValue = session
                        .advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(Integer.class, "cmd/null");
                assertThat(nullValue.getValue())
                        .isNull();
            }
        }
    }
}
