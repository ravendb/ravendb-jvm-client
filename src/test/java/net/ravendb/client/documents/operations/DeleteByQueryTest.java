package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeleteByQueryTest extends RemoteTestBase {

    @Test
    public void canDeleteByQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setAge(5);
                session.store(user1);

                User user2 = new User();
                user2.setAge(10);
                session.store(user2);

                session.saveChanges();
            }

            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setQuery("from users where age == 5");
            DeleteByQueryOperation operation = new DeleteByQueryOperation(indexQuery);
            Operation asyncOp = store.operations().sendAsync(operation);

            asyncOp.waitForCompletion();

            try (IDocumentSession session = store.openSession()) {
                Assertions.assertThat(session.query(User.class)
                        .count())
                        .isEqualTo(1);
            }
        }
    }
}
