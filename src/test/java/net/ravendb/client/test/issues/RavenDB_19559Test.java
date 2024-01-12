package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeResult;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.GetCompareExchangeValueOperation;
import net.ravendb.client.documents.operations.compareExchange.PutCompareExchangeValueOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_19559Test extends RemoteTestBase {

    @Test
    public void can_Use_Arrays_In_CompareExchange() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction().createCompareExchangeValue("key2", new String[] { "1", "2", "3" });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<String[]> value = session.advanced().clusterTransaction().getCompareExchangeValue(String[].class, "key2");
                value.getValue()[2] = "4";
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<String[]> value = session.advanced().clusterTransaction().getCompareExchangeValue(String[].class, "key2");
                assertThat(value.getValue())
                        .hasSize(3);
                assertThat(value.getValue())
                        .containsSequence("1", "2", "4");
            }

            CompareExchangeResult<String[]> result1 = store.operations().send(new PutCompareExchangeValueOperation<>("key1", new String[]{"1", "2", "3"}, 0));
            assertThat(result1.getValue())
                    .isEqualTo(new String[] { "1", "2", "3"});

            CompareExchangeValue<String[]> result2 = store.operations().send(new GetCompareExchangeValueOperation<>(String[].class, "key1"));
            assertThat(result2.getValue())
                    .isEqualTo(new String[] { "1", "2", "3"});
        }
    }

}
