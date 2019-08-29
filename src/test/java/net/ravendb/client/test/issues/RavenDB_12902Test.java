package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_12902Test extends RemoteTestBase {

    @Test
    public void afterAggregationQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new UsersByName());

            try (IDocumentSession session = store.openSession()) {
                AtomicInteger counter = new AtomicInteger();

                Reference<QueryStatistics> stats = new Reference<>();

                Map<String, FacetResult> results = session.query(User.class, UsersByName.class)
                        .statistics(stats)
                        .addAfterQueryExecutedListener(x -> {
                            counter.incrementAndGet();
                        })
                        .whereEquals("name", "Doe")
                        .aggregateBy(x -> x.byField("name").sumOn("count"))
                        .execute();

                assertThat(results)
                        .hasSize(1);
                assertThat(results.get("name").getValues().size())
                        .isZero();
                assertThat(stats.value)
                        .isNotNull();
                assertThat(counter.get())
                        .isOne();
            }
        }
    }

    @Test
    public void afterSuggestionQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new UsersByName());

            try (IDocumentSession session = store.openSession()) {
                AtomicInteger counter = new AtomicInteger();

                Reference<QueryStatistics> stats = new Reference<>();

                Map<String, SuggestionResult> results = session.query(User.class, UsersByName.class)
                        .statistics(stats)
                        .addAfterQueryExecutedListener(x -> {
                            counter.incrementAndGet();
                        })
                        .suggestUsing(x -> x.byField("name", "Orin"))
                        .execute();

                assertThat(results)
                        .hasSize(1);
                assertThat(results.get("name").getSuggestions().size())
                        .isZero();
                assertThat(stats.value)
                        .isNotNull();
                assertThat(counter.get())
                        .isOne();
            }
        }
    }

    @Test
    public void afterLazyQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                AtomicInteger counter = new AtomicInteger();

                Reference<QueryStatistics> stats = new Reference<>();

                List<User> results = session.query(User.class)
                        .addAfterQueryExecutedListener(x -> {
                            counter.incrementAndGet();
                        })
                        .statistics(stats)
                        .whereEquals("name", "Doe")
                        .lazily()
                        .getValue();

                assertThat(results)
                        .isEmpty();
                assertThat(stats.value)
                        .isNotNull();
                assertThat(counter.get())
                        .isOne();
            }
        }
    }

    @Test
    public void afterLazyAggregationQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new UsersByName());

            try (IDocumentSession session = store.openSession()) {
                AtomicInteger counter = new AtomicInteger();

                Reference<QueryStatistics> stats = new Reference<>();

                Map<String, FacetResult> results = session.query(User.class, UsersByName.class)
                        .statistics(stats)
                        .addAfterQueryExecutedListener(x -> {
                            counter.incrementAndGet();
                        })
                        .whereEquals("name", "Doe")
                        .aggregateBy(x -> x.byField("name").sumOn("count"))
                        .executeLazy()
                        .getValue();

                assertThat(results)
                        .hasSize(1);
                assertThat(results.get("name").getValues().size())
                        .isZero();
                assertThat(stats.value)
                        .isNotNull();
                assertThat(counter.get())
                        .isOne();
            }
        }
    }

    @Test
    public void afterLazySuggestionQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new UsersByName());

            try (IDocumentSession session = store.openSession()) {
                AtomicInteger counter = new AtomicInteger();

                Reference<QueryStatistics> stats = new Reference<>();

                Map<String, SuggestionResult> results = session
                        .query(User.class, UsersByName.class)
                        .addAfterQueryExecutedListener(r -> {
                            counter.incrementAndGet();
                        })
                        .statistics(stats)
                        .suggestUsing(x -> x.byField("name", "Orin"))
                        .executeLazy()
                        .getValue();

                assertThat(results)
                        .hasSize(1);
                assertThat(results.get("name").getSuggestions().size())
                        .isZero();
                assertThat(stats.value)
                        .isNotNull();
                assertThat(counter.get())
                        .isOne();
            }
        }
    }

    public static class UsersByName extends AbstractIndexCreationTask {
        public UsersByName() {
            map = "from u in docs.Users select new " +
                    " {" +
                    "    firstNAme = u.name, " +
                    "    lastName = u.lastName" +
                    "}";

            suggestion("name");
        }
    }
}
