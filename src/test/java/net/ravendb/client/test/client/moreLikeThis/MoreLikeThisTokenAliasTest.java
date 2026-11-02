package net.ravendb.client.test.client.moreLikeThis;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.FieldTermVector;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisOptions;
import net.ravendb.client.documents.session.AbstractDocumentQuery;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.tokens.MoreLikeThisToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MoreLikeThisTokenAliasTest extends RemoteTestBase {

    @Test
    public void moreLikeThisTokenIsNotAliased() {
        MoreLikeThisToken token = new MoreLikeThisToken();

        assertThat(token.addAlias("d")).isSameAs(token);
    }

    @Test
    public void moreLikeThisTokenSurvivesFromAliasWhenProjectionIsJsObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new Article("test test test"));
                session.store(new Article("cake is great"));
                session.saveChanges();
            }

            new ArticleIndex().execute(store);
            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setMinimumTermFrequency(1);
                options.setMinimumDocumentFrequency(1);
                options.setMinimumWordLength(0);

                IDocumentQuery<ArticleResult> query = session.query(Article.class, ArticleIndex.class)
                        .moreLikeThis(f -> f.usingDocument("{\"body\":\"test\"}").withOptions(options))
                        .selectFields(ArticleResult.class, QueryData.customFunction("d",
                                "{ id: id(d), body: d.body, meta: d['@metadata']['@last-modified'] }"));

                // what the C# LINQ provider does for a JS projection with a from-alias
                ((AbstractDocumentQuery<?, ?>) query).addFromAliasToWhereTokens("d");

                assertThat(query.toString()).contains("moreLikeThis(");

                List<ArticleResult> results = query.toList();

                assertThat(results).hasSize(1);
                assertThat(results.get(0).getBody()).isEqualTo("test test test");
                assertThat(results.get(0).getMeta()).isNotNull();
            }
        }
    }

    public static class Article {
        private String id;
        private String body;

        public Article() {
        }

        public Article(String body) {
            this.body = body;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class ArticleResult {
        private String id;
        private String body;
        private Object meta;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Object getMeta() {
            return meta;
        }

        public void setMeta(Object meta) {
            this.meta = meta;
        }
    }

    public static class ArticleIndex extends AbstractIndexCreationTask {
        public ArticleIndex() {
            map = "from a in docs.Articles select new { a.body }";
            index("body", FieldIndexing.SEARCH);
            termVector("body", FieldTermVector.YES);
        }
    }
}
