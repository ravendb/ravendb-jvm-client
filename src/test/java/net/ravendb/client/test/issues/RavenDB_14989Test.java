package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.CompareExchangeKeyTooBigException;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_14989Test extends RemoteTestBase {

    @Test
    public void shouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User user = new User();
                user.setName("egor");
                session.advanced().clusterTransaction()
                        .createCompareExchangeValue(StringUtils.repeat('e', 513), user);

                assertThatThrownBy(session::saveChanges)
                        .isExactlyInstanceOf(CompareExchangeKeyTooBigException.class);
            }
        }
    }
}
