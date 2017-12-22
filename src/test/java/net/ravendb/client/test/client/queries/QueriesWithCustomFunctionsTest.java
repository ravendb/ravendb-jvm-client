package net.ravendb.client.test.client.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.PutCompareExchangeValueOperation;
import net.ravendb.client.documents.session.CmpXchg;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QueriesWithCustomFunctionsTest extends RemoteTestBase {

    @Test
    public void queryCmpXchgWhere() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.operations().send(new PutCompareExchangeValueOperation<>("Tom", "Jerry", 0));
            store.operations().send(new PutCompareExchangeValueOperation<>("Hera", "Zeus", 0));
            store.operations().send(new PutCompareExchangeValueOperation<>("Gaya", "Uranus", 0));
            store.operations().send(new PutCompareExchangeValueOperation<>("Jerry@gmail.com", "users/2", 0));
            store.operations().send(new PutCompareExchangeValueOperation<>("Zeus@gmail.com", "users/1", 0));

            try (IDocumentSession session = store.openSession()) {
                User jerry = new User();
                jerry.setName("Jerry");
                session.store(jerry, "users/2");
                session.saveChanges();

                User zeus = new User();
                zeus.setName("Zeus");
                zeus.setLastName("Jerry");
                session.store(zeus, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> q = session.advanced()
                        .documentQuery(User.class)
                        .whereEquals("name", CmpXchg.value("Hera"))
                        .whereEquals("lastName", CmpXchg.value("Tom"));

                assertThat(q.getIndexQuery().getQuery())
                        .isEqualTo("from Users where name = cmpxchg($p0) and lastName = cmpxchg($p1)");

                List<User> queryResult = q.toList();
                assertThat(queryResult)
                        .hasSize(1);
                assertThat(queryResult.get(0).getName())
                        .isEqualTo("Zeus");

                List<User> user = session.advanced().documentQuery(User.class)
                        .whereNotEquals("name", CmpXchg.value("Hera"))
                        .toList();

                assertThat(user)
                        .hasSize(1);

                assertThat(user.get(0).getName())
                        .isEqualTo("Jerry");

                List<User> users = session.advanced().rawQuery(User.class, "from Users where name = cmpxchg(\"Hera\")")
                        .toList();

                assertThat(users)
                        .hasSize(1);

                assertThat(users.get(0).getName())
                        .isEqualTo("Zeus");
            }
        }
    }
}
