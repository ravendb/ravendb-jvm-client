package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.infrastructure.entities.Employee;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_21264Test extends RemoteTestBase {

    @Test
    public void shouldSetSkipStatisticsAccordingly() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IndexQuery indexQuery = session.advanced().documentQuery(Employee.class)
                        .whereStartsWith("firstName", "bob")
                        .orderBy("birthday")
                        .getIndexQuery();

                assertThat(indexQuery.isSkipStatistics())
                        .isTrue();
            }

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                IndexQuery indexQuery = session.advanced().documentQuery(Employee.class)
                        .statistics(statsRef)
                        .whereStartsWith("firstName", "bob")
                        .orderBy("birthday")
                        .getIndexQuery();

                assertThat(indexQuery.isSkipStatistics())
                        .isFalse();
            }
        }
    }
}
