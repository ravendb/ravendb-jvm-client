package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15282Test extends RemoteTestBase {

    @Test
    public void countersPostGetReturnFullResults() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String docId = "users/1";
            String[] counterNames = new String[1000];

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);

                ISessionDocumentCounters c = session.countersFor(docId);

                for (int i = 0; i < 1000; i++) {
                    String name = "likes" + i;
                    counterNames[i] = name;
                    c.increment(name);
                }

                session.saveChanges();
            }

            CountersDetail vals = store.operations().send(new GetCountersOperation(docId, counterNames, true));
            assertThat(vals.getCounters())
                    .hasSize(1000);

            for (int i = 0; i < 1000; i++) {
                assertThat(vals.getCounters().get(i).getCounterValues())
                        .hasSize(1);
                assertThat(vals.getCounters().get(i).getCounterValues().values().iterator().next())
                        .isEqualTo(1L);
            }
        }
    }
}
