package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.1")
public class RavenDB_24620Test extends RemoteTestBase {

    @Test
    public void canUseTaskToQueryPregeneratedEmbedding_rql() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String rql = session.query(User.class)
                        .vectorSearch(f -> f.withField("Vector"), v -> v.byText("car", "localaitask"))
                        .toString();
                assertThat(rql).isEqualTo("from 'Users' where vector.search(Vector, embedding.text($p0, ai.task('localaitask')))");

                rql = session.query(User.class)
                        .vectorSearch(f -> f.withField("Vector"), v -> v.byTexts(Arrays.asList("car", "planet"), "localaitask"))
                        .toString();
                assertThat(rql).isEqualTo("from 'Users' where vector.search(Vector, embedding.text($p0, ai.task('localaitask')))");
            }
        }
    }

    @Test
    public void cannotUseTaskIdentifierOnBothFieldAndValue() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.query(User.class)
                        .vectorSearch(f -> f.withText("Name").usingTask("field-task"), v -> v.byText("car", "value-task")))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Embeddings generation task identifier set in value factory cannot be used with field factory. It solely purpose to use already generated embeddings.");
            }
        }
    }
}
