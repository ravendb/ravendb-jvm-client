package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.session.AbstractDocumentQuery;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.tokens.VectorSearchToken;
import net.ravendb.client.documents.session.tokens.WhereOperator;
import net.ravendb.client.documents.session.tokens.WhereToken;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.1")
public class VectorSearchTokenAliasTest extends RemoteTestBase {

    @Test
    public void addAliasKeepsVectorSearchTokenAndOptions() {
        VectorSearchToken token = new VectorSearchToken("Vector", "p0",
                VectorEmbeddingType.SINGLE, VectorEmbeddingType.SINGLE, 0.5f, null, true, false, null, null);
        token.getOptions().setBoost(2.0);
        token.setWhereOperator(WhereOperator.VECTOR_SEARCH);

        WhereToken aliased = token.addAlias("d");

        assertThat(aliased).isInstanceOf(VectorSearchToken.class).isNotSameAs(token);
        assertThat(aliased.getFieldName()).isEqualTo("d.Vector");
        assertThat(aliased.getParameterName()).isEqualTo("p0");
        assertThat(aliased.getOptions()).isSameAs(token.getOptions());
        assertThat(aliased.getWhereOperator()).isEqualTo(WhereOperator.VECTOR_SEARCH);

        StringBuilder rql = new StringBuilder();
        aliased.writeTo(rql);
        assertThat(rql.toString()).isEqualTo("boost(exact(vector.search(d.Vector, $p0, 0.5, null)), 2.0)");
    }

    @Test
    public void addAliasSkipsDocumentIdField() {
        VectorSearchToken token = new VectorSearchToken("id()", "p0",
                VectorEmbeddingType.SINGLE, VectorEmbeddingType.SINGLE, null, null, false, false, null, null);

        assertThat(token.addAlias("d")).isSameAs(token);
    }

    @Test
    public void vectorSearchTokenSurvivesFromAliasWhenProjectionIsJsObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User match = new User();
                match.setName("Ali");
                session.store(match);

                User other = new User();
                other.setName("Car");
                session.store(other);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<UserResult> query = session.query(User.class)
                        .vectorSearch(x -> x.withText("name"), v -> v.byText("Ali"), 0.75f, null, null)
                        .selectFields(UserResult.class, QueryData.customFunction("d", "{ name: d.name }"));

                // what the C# LINQ provider does for a JS projection with a from-alias
                ((AbstractDocumentQuery<?, ?>) query).addFromAliasToWhereTokens("d");

                String rql = query.toString();
                assertThat(rql).contains("vector.search(embedding.text(d.name), $p0, 0.75, null)");
                assertThat(rql).doesNotContain("d.name = ");

                List<UserResult> results = query.toList();
                assertThat(results).extracting(UserResult::getName).containsExactly("Ali");
            }
        }
    }

    @Test
    public void exactVectorSearchTokenSurvivesFromAliasWhenProjectionIsJsObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User match = new User();
                match.setName("Ali");
                session.store(match);

                User other = new User();
                other.setName("Car");
                session.store(other);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<UserResult> query = session.query(User.class)
                        .vectorSearch(x -> x.withText("name"), v -> v.byText("Ali"), 0.75f, null, true)
                        .selectFields(UserResult.class, QueryData.customFunction("d", "{ name: d.name }"));

                ((AbstractDocumentQuery<?, ?>) query).addFromAliasToWhereTokens("d");

                String rql = query.toString();
                assertThat(rql).contains("exact(vector.search(embedding.text(d.name), $p0, 0.75, null))");
                assertThat(rql).doesNotContain("d.name = ");

                List<UserResult> results = query.toList();
                assertThat(results).extracting(UserResult::getName).containsExactly("Ali");
            }
        }
    }

    public static class UserResult {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
