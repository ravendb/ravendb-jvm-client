package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14919Test extends RemoteTestBase {

    @Test
    public void getCountersOperationShouldDiscardNullCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String docId = "users/2";

            String[] counterNames = new String[124];

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);

                ISessionDocumentCounters c = session.countersFor(docId);
                for (int i = 0; i < 100; i++) {
                    String name = "likes" + i;
                    counterNames[i] = name;
                    c.increment(name);
                }

                session.saveChanges();
            }

            CountersDetail vals = store.operations().send(new GetCountersOperation(docId, counterNames));
            assertThat(vals.getCounters())
                    .hasSize(101);

            for (int i = 0; i < 100; i++) {
                assertThat(vals.getCounters().get(i).getTotalValue())
                        .isEqualTo(1);
            }

            assertThat(vals.getCounters().get(vals.getCounters().size() - 1))
                    .isNull();

            // test with returnFullResults = true
            vals = store.operations().send(new GetCountersOperation(docId, counterNames, true));

            assertThat(vals.getCounters())
                    .hasSize(101);

            for (int i = 0; i < 100; i++) {
                assertThat(vals.getCounters().get(i).getCounterValues())
                        .hasSize(1);
            }

            assertThat(vals.getCounters().get(vals.getCounters().size() - 1))
                    .isNull();
        }
    }

    @Test
    public void getCountersOperationShouldDiscardNullCounters_PostGet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String docId = "users/2";
            String[] counterNames = new String[1024];

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);

                ISessionDocumentCounters c = session.countersFor(docId);

                for (int i = 0; i < 1000; i++) {
                    String name = "likes" + i;
                    counterNames[i] = name;
                    c.increment(name, i);
                }

                session.saveChanges();
            }

            CountersDetail vals = store.operations().send(new GetCountersOperation(docId, counterNames));
            assertThat(vals.getCounters())
                    .hasSize(1001);

            for (int i = 0; i < 1000; i++) {
                assertThat(vals.getCounters().get(i).getTotalValue())
                        .isEqualTo(i);
            }

            assertThat(vals.getCounters().get(vals.getCounters().size() - 1))
                    .isNull();

            // test with returnFullResults = true
            vals = store.operations().send(new GetCountersOperation(docId, counterNames, true));
            assertThat(vals.getCounters())
                    .hasSize(1001);

            for (int i = 0; i < 1000; i++) {
                assertThat(vals.getCounters().get(i).getTotalValue())
                        .isEqualTo(i);
            }

            assertThat(vals.getCounters().get(vals.getCounters().size() - 1))
                    .isNull();
        }
    }

    @Test
    public void getDocumentsCommandShouldDiscardNullIds() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] ids = new String[124];

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    String id = "users/" + i;
                    ids[i] = id;
                    session.store(new User(), id);
                }

                session.saveChanges();
            }

            RequestExecutor re = store.getRequestExecutor();
            GetDocumentsCommand command = new GetDocumentsCommand(ids, null, false);
            re.execute(command);

            assertThat(command.getResult().getResults())
                    .hasSize(101);
            assertThat(command.getResult().getResults().get(command.getResult().getResults().size() - 1).isNull())
                    .isTrue();
        }
    }

    @Test
    public void getDocumentsCommandShouldDiscardNullIds_PostGet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] ids = new String[1024];

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 1000; i++) {
                    String id = "users/" + i;
                    ids[i] = id;
                    session.store(new User(), id);
                }

                session.saveChanges();
            }


            RequestExecutor re = store.getRequestExecutor();
            GetDocumentsCommand command = new GetDocumentsCommand(ids, null, false);
            re.execute(command);

            assertThat(command.getResult().getResults())
                    .hasSize(1001);
            assertThat(command.getResult().getResults().get(command.getResult().getResults().size() - 1).isNull())
                    .isTrue();
        }
    }
}
