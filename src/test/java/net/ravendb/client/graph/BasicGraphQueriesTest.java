package net.ravendb.client.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.documents.smuggler.DatabaseItemType;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import net.ravendb.client.infrastructure.DisabledOn60Server;
import net.ravendb.client.infrastructure.graph.Movie;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOn60Server
public class BasicGraphQueriesTest extends RemoteTestBase {

    public static class StalenessParameters {
        public static final StalenessParameters defaultParameters = new StalenessParameters();

        static {
            defaultParameters.setWaitForIndexing(true);
            defaultParameters.setWaitForNonStaleResults(false);
            defaultParameters.setWaitForNonStaleResultsDuration(Duration.ofSeconds(15));
        }

        private boolean waitForIndexing;
        private boolean waitForNonStaleResults;
        private Duration waitForNonStaleResultsDuration;

        public boolean isWaitForIndexing() {
            return waitForIndexing;
        }

        public void setWaitForIndexing(boolean waitForIndexing) {
            this.waitForIndexing = waitForIndexing;
        }

        public boolean isWaitForNonStaleResults() {
            return waitForNonStaleResults;
        }

        public void setWaitForNonStaleResults(boolean waitForNonStaleResults) {
            this.waitForNonStaleResults = waitForNonStaleResults;
        }

        public Duration getWaitForNonStaleResultsDuration() {
            return waitForNonStaleResultsDuration;
        }

        public void setWaitForNonStaleResultsDuration(Duration waitForNonStaleResultsDuration) {
            this.waitForNonStaleResultsDuration = waitForNonStaleResultsDuration;
        }
    }

    private <T> List<T> query(Class<T> clazz, String q, Consumer<IDocumentStore> mutate, StalenessParameters parameters) throws Exception {
        if (parameters == null) {
            parameters = StalenessParameters.defaultParameters;
        }

        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(
                    new CreateSampleDataOperation(EnumSet.of(DatabaseItemType.DOCUMENTS, DatabaseItemType.INDEXES)));

            if (mutate != null) {
                mutate.accept(store);
            }

            if (parameters.isWaitForIndexing()) {
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<T> query = session.advanced().rawQuery(clazz, q);
                if (parameters.isWaitForNonStaleResults()) {
                    query = query.waitForNonStaleResults(parameters.getWaitForNonStaleResultsDuration());
                }

                return query.toList();
            }
        }
    }

    @Test
    public void query_with_no_matches_and_select_should_return_empty_result() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createDogDataWithoutEdges(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> results = session.advanced().rawQuery(ObjectNode.class, " match (Dogs as a)-[Likes]->(Dogs as f)<-[Likes]-(Dogs as b)\n" +
                        " select {\n" +
                        "     a: a,\n" +
                        "     f: f,\n" +
                        "     b: b\n" +
                        " }").toList();

                assertThat(results)
                        .isEmpty();
            }
        }
    }

    @Test
    public void query_with_no_matches_and_without_select_should_return_empty_result() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createDogDataWithoutEdges(store);
            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> results = session.advanced().rawQuery(ObjectNode.class, "match (Dogs as a)-[Likes]->(Dogs as f)<-[Likes]-(Dogs as b)").toList();
                assertThat(results)
                        .isEmpty();
            }
        }
    }

    @Test
    public void empty_vertex_node_should_work() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Movie> results = session.advanced().rawQuery(Movie.class, "match ()-[hasRated select movie]->(Movies as m) select m").toList();
                assertThat(results)
                        .hasSize(5);
            }
        }
    }

    @Test
    public void can_flatten_result_for_single_vertex_in_row() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> allVerticesQuery = session.advanced().rawQuery(ObjectNode.class, "match (_ as v)").toList();
                assertThat(allVerticesQuery)
                        .anySatisfy(x -> assertThat(x.get("_ as v")).isNull());
            }
        }
    }

    @Test
    public void mutliple_results_in_row_wont_flatten_results() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> allVerticesQuery = session
                        .advanced()
                        .rawQuery(ObjectNode.class, "match (_ as u)-[HasRated select Movie]->(_ as m)")
                        .toList();

                assertThat(allVerticesQuery)
                        .allMatch(x -> x.get("m") != null)
                        .allMatch(x -> x.get("u") != null);
            }
        }
    }

    @Test
    public void can_query_without_collection_identifier() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> allVerticesQuery = session.advanced().rawQuery(ObjectNode.class, "match (_ as v)").toList();

                assertThat(allVerticesQuery)
                        .hasSize(9);

                List<String> docTypes = allVerticesQuery
                        .stream()
                        .map(x -> x.get("@metadata").get("@collection").asText())
                        .collect(Collectors.toList());

                assertThat(docTypes.stream().filter(x -> x.equals("Genres")).count())
                        .isEqualTo(3);
                assertThat(docTypes.stream().filter(x -> x.equals("Movies")).count())
                        .isEqualTo(3);
                assertThat(docTypes.stream().filter(x -> x.equals("Users")).count())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void can_use_explicit_with_clause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> results = session.advanced().rawQuery(ObjectNode.class, "  with {from Users} as u match (u)")
                        .toList();

                assertThat(results)
                        .hasSize(3);

                List<String> docTypes = results
                        .stream()
                        .map(x -> x.get("@metadata").get("@collection").asText())
                        .collect(Collectors.toList());

                assertThat(docTypes)
                        .allMatch("Users"::equals);
            }
        }
    }

    @Test
    public void can_filter_vertices_with_explicit_with_clause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createMoviesData(store);

            try (IDocumentSession session = store.openSession()) {
                List<String> results = session
                        .advanced()
                        .rawQuery(ObjectNode.class, "with {from Users where id() = 'users/2'} as u match (u) select u.name")
                        .toList()
                        .stream()
                        .map(x -> x.get("name").asText())
                        .collect(Collectors.toList());

                assertThat(results)
                        .hasSize(1);

                assertThat(results.get(0))
                        .isEqualTo("Jill");

            }
        }
    }

    @Test
    public void findReferences() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            samples.createSimpleData(store);

            try (IDocumentSession session = store.openSession()) {
                List<ObjectNode> result = session
                        .advanced()
                        .rawQuery(ObjectNode.class, "match (Entities as e)-[references as r]->(Entities as e2)")
                        .toList();

                assertThat(result)
                        .hasSize(3)
                        .anySatisfy(item -> {
                            assertThat(item.get("e").get("name").asText())
                                    .isEqualTo("A");
                            assertThat(item.get("e2").get("name").asText())
                                    .isEqualTo("B");
                        })
                        .anySatisfy(item -> {
                            assertThat(item.get("e").get("name").asText())
                                    .isEqualTo("B");
                            assertThat(item.get("e2").get("name").asText())
                                    .isEqualTo("C");
                        })
                        .anySatisfy(item -> {
                            assertThat(item.get("e").get("name").asText())
                                    .isEqualTo("C");
                            assertThat(item.get("e2").get("name").asText())
                                    .isEqualTo("A");
                        });
            }
        }
    }
}
