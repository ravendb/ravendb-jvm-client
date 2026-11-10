package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.RavenVector;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.0")
public class RavenDB_23076Test extends RemoteTestBase {

    @Test
    public void testRavenVectorStorageAndLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RavenVector<Float> vectorToStore = new RavenVector<>(new Float[]{0.1f, 0.2f});

            try (IDocumentSession session = store.openSession()) {
                Dto dto = new Dto();
                dto.setId("Dtos/1");
                dto.setVector(vectorToStore);

                session.store(dto);
                session.saveChanges();

                IndexWithServerSideType index = new IndexWithServerSideType();
                index.execute(store);

                waitForIndexing(store);

                List<Dto> entries = session.query(IndexEntry.class, IndexWithServerSideType.class)
                        .whereEquals("UnderlyingArrayType", "Sparrow.Json.BlittableJsonReaderVector")
                        .selectFields(Dto.class)
                        .toList();

                assertThat(entries).hasSize(1);
            }

            try (IDocumentSession session = store.openSession()) {
                Dto loadedDto = session.load(Dto.class, "Dtos/1");

                assertThat(loadedDto.getVector().getEmbedding())
                        .containsExactly(vectorToStore.getEmbedding());

                // loading must not mark the entity as changed
                assertThat(session.advanced().hasChanges()).isFalse();
            }

            try (IDocumentSession session = store.openSession()) {
                List<Dto> results = session.query(Dto.class)
                        .vectorSearch(f -> f.withEmbedding("vector"), v -> v.byEmbedding(vectorToStore))
                        .toList();

                assertThat(results).hasSize(1);
            }
        }
    }

    @Test
    public void ravenVectorSerializesAsVectorObject() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultEntityMapper();

        Dto dto = new Dto();
        dto.setVector(new RavenVector<>(new Float[]{0.5f, 1.5f}));

        ObjectNode json = mapper.valueToTree(dto);
        assertThat(json.get("vector").toString()).isEqualTo("{\"@vector\":[0.5,1.5]}");

        Dto read = mapper.treeToValue(json, Dto.class);
        assertThat(read.getVector().getEmbedding()).containsExactly(0.5f, 1.5f);

        IntDto intDto = mapper.readValue("{\"vector\":{\"@vector\":[1,-2,3]}}", IntDto.class);
        assertThat(intDto.getVector().getEmbedding()).containsExactly(1, -2, 3);
    }

    @Test
    public void ravenVectorRejectsNonNumericEmbeddingType() {
        assertThatThrownBy(() -> new RavenVector<Number>(new Number[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The type of embedding must be numeric");
    }

    public static class Dto {
        private String id;
        private RavenVector<Float> vector;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public RavenVector<Float> getVector() {
            return vector;
        }

        public void setVector(RavenVector<Float> vector) {
            this.vector = vector;
        }
    }

    public static class IntDto {
        private RavenVector<Integer> vector;

        public RavenVector<Integer> getVector() {
            return vector;
        }

        public void setVector(RavenVector<Integer> vector) {
            this.vector = vector;
        }
    }

    public static class IndexEntry {
        private String underlyingArrayType;

        public String getUnderlyingArrayType() {
            return underlyingArrayType;
        }

        public void setUnderlyingArrayType(String underlyingArrayType) {
            this.underlyingArrayType = underlyingArrayType;
        }
    }

    public static class IndexWithServerSideType extends AbstractIndexCreationTask {
        public IndexWithServerSideType() {
            map = "from dto in docs.Dtos " +
                    "select new { UnderlyingArrayType = ((Raven.Server.Documents.Indexes.Static.DynamicBlittableJson)(object)dto.vector)[\"@vector\"].GetType().FullName }";
        }
    }
}
