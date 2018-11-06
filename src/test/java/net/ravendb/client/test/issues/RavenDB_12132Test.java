package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.BatchPatchCommandData;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeResult;
import net.ravendb.client.documents.operations.compareExchange.PutCompareExchangeValueOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_12132Test extends RemoteTestBase {

    @Test
    public void canPutObjectWithId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setId("users/1");
            user.setName("Grisha");

            CompareExchangeResult<User> res = store.operations().send(new PutCompareExchangeValueOperation<User>("test", user, 0));

            assertThat(res.isSuccessful())
                    .isTrue();

            assertThat(res.getValue().getName())
                    .isEqualTo("Grisha");
            assertThat(res.getValue().getId())
                    .isEqualTo("users/1");
        }
    }

    @Test
    public void canCreateClusterTransactionRequest1() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setId("this/is/my/id");
            user.setName("Grisha");

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.advanced().clusterTransaction()
                        .createCompareExchangeValue("usernames/ayende", user);
                session.saveChanges();

                User userFromCluster = session.advanced()
                        .clusterTransaction()
                        .getCompareExchangeValue(User.class,"usernames/ayende").getValue();
                assertThat(userFromCluster.getName())
                        .isEqualTo(user.getName());
                assertThat(userFromCluster.getId())
                        .isEqualTo(user.getId());
            }
        }
    }
}
