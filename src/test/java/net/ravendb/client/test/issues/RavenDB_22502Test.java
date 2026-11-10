package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.util.ClientChangeVectorUtils;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22502Test extends RemoteTestBase {

    @Test
    public void savingClusterTransactionDocumentRaisesLastClusterTransactionIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User user = new User();
                user.setName("Grisha");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");

                SessionInfo sessionInfo = session.advanced().getSessionInfo();
                String clusterTransactionId = sessionInfo.getClusterTransactionId();
                assertThat(clusterTransactionId)
                        .isNotNull();

                String changeVector = session.advanced().getChangeVectorFor(user);
                long clusterTxIndex = ClientChangeVectorUtils.getEtagById(changeVector, clusterTransactionId);
                assertThat(clusterTxIndex)
                        .isPositive();

                sessionInfo.setLastClusterTransactionIndex(null);

                List<String> knownRaftIndexes = new ArrayList<>();
                store.getRequestExecutor().addOnBeforeRequestListener((sender, event) -> {
                    if (event.getUrl().contains("/bulk_docs")) {
                        Header header = event.getRequest().getFirstHeader(Constants.Headers.LAST_KNOWN_CLUSTER_TRANSACTION_INDEX);
                        knownRaftIndexes.add(header != null ? header.getValue() : null);
                    }
                });

                user.setName("Grisha2");
                session.saveChanges();

                // the batch request waits for the cluster transaction index taken from the document change vector
                assertThat(knownRaftIndexes)
                        .containsExactly(String.valueOf(clusterTxIndex));
            }
        }
    }
}
