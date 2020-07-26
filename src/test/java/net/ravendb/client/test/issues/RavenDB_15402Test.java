package net.ravendb.client.test.issues;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15402Test extends ReplicationTestBase {

    @Test
    public void getCountersShouldBeCaseInsensitive() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "companies/1";

            try (IDocumentSession session = store.openSession()) {
                session.store(new Company(), id);
                session.countersFor(id)
                        .increment("Likes", 999);
                session.countersFor(id)
                        .increment("DisLikes", 999);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, id);
                Map<String, Long> counters = session.countersFor(company)
                        .get(Arrays.asList("likes", "dislikes"));

                assertThat(counters)
                        .hasSize(2);
                assertThat(counters.get("likes"))
                        .isEqualTo(999);
                assertThat(counters.get("dislikes"))
                        .isEqualTo(999);
            }
        }
    }
}
