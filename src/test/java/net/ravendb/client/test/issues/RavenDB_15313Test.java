package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15313Test extends RemoteTestBase {

    @Test
    public void getCountersOperationShouldFilterDuplicateNames() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String docId = "users/1";

            String[] names = new String[] { "likes", "dislikes", "likes", "downloads", "likes", "downloads" };

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);

                ISessionDocumentCounters cf = session.countersFor(docId);

                for (int i = 0; i < names.length; i++) {
                    cf.increment(names[i], i);
                }

                session.saveChanges();
            }

            CountersDetail vals = store.operations().send(new GetCountersOperation(docId, names));

            assertThat(vals.getCounters())
                    .hasSize(3);

            int expected = 6; // likes
            assertThat(vals.getCounters().get(0).getTotalValue())
                    .isEqualTo(expected);

            expected = 1; // dislikes
            assertThat(vals.getCounters().get(1).getTotalValue())
                    .isEqualTo(expected);

            expected = 8;
            assertThat(vals.getCounters().get(2).getTotalValue())
                    .isEqualTo(expected);
        }
    }

    @Test
    public void getCountersOperationShouldFilterDuplicateNames_PostGet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String docId = "users/1";

            String[] names = new String[1024];
            Map<String, Integer> dict = new HashMap<>();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);;

                ISessionDocumentCounters cf = session.countersFor(docId);

                for (int i = 0; i < 1024; i++) {
                    String name;
                    if (i % 4 == 0) {
                        name = "abc";
                    } else if (i % 10 == 0) {
                        name = "xyz";
                    } else {
                        name = "likes" + i;
                    }

                    names[i] = name;

                    Integer oldVal = dict.get(name);
                    dict.put(name, oldVal != null ? oldVal + i : i);

                    cf.increment(name, i);
                }

                session.saveChanges();
            }

            CountersDetail vals = store.operations().send(new GetCountersOperation(docId, names));

            int expectedCount = dict.size();

            assertThat(vals.getCounters())
                    .hasSize(expectedCount);

            List<String> namesList = Arrays.asList(names);
            Set<String> hs = new HashSet<>(namesList);
            List<Integer> expectedVals = namesList
                    .stream()
                    .filter(hs::remove)
                    .map(dict::get)
                    .collect(Collectors.toList());

            for (int i = 0; i < vals.getCounters().size(); i++) {
                assertThat((long)expectedVals.get(i))
                        .isEqualTo(vals.getCounters().get(i).getTotalValue());
            }
        }
    }
}
