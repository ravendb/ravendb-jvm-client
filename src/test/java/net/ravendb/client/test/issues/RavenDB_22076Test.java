package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.AutoIndexDefinition;
import net.ravendb.client.documents.indexes.AutoIndexFieldOptions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.operations.indexes.GetIndexesOperation;
import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.0")
public class RavenDB_22076Test extends RemoteTestBase {

    @Test
    public void testRqlGeneration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String q1 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8), f -> f.byEmbedding(new Float[]{2.5f, 3.3f}), 0.65f, 12)
                        .toString();
                assertThat(q1).isEqualTo("from 'Dtos' where vector.search(embedding.i8(EmbeddingField), $p0, 0.65, 12)");

                String q2 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withField("VectorField"), f -> f.byText("aaaa"))
                        .toString();
                assertThat(q2).isEqualTo("from 'Dtos' where vector.search(VectorField, $p0)");

                String q3 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withField("VectorField"), f -> f.byEmbedding(Arrays.asList(0.3f, 0.4f, 0.5f)))
                        .toString();
                assertThat(q3).isEqualTo("from 'Dtos' where vector.search(VectorField, $p0)");

                String q4 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withField("VectorField"), f -> f.byBase64("aaaa=="))
                        .toString();
                assertThat(q4).isEqualTo("from 'Dtos' where vector.search(VectorField, $p0)");

                String q5 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withText("TextField").targetQuantization(VectorEmbeddingType.INT8), f -> f.byText("aaaa"))
                        .toString();
                assertThat(q5).isEqualTo("from 'Dtos' where vector.search(embedding.text_i8(TextField), $p0)");

                String q6 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8), f -> f.byEmbedding(new Float[]{2.5f, 3.3f}), 0.65f)
                        .toString();
                assertThat(q6).isEqualTo("from 'Dtos' where vector.search(embedding.i8(EmbeddingField), $p0, 0.65, null)");
            }
        }
    }

    @Test
    public void testRqlGeneration2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8).targetQuantization(VectorEmbeddingType.BINARY), f -> f.byEmbedding(new Float[]{2.5f, 3.3f}), 0.65f)
                        .toString())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Cannot quantize already quantized embeddings");

                assertThatThrownBy(() -> session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8).targetQuantization(VectorEmbeddingType.SINGLE), f -> f.byEmbedding(new Float[]{2.5f, 3.3f}), 0.65f)
                        .toString())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Cannot quantize already quantized embeddings");

                String q2 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField").targetQuantization(VectorEmbeddingType.INT8), f -> f.byEmbedding(new Float[]{2.5f, 3.3f}), 0.65f)
                        .toString();
                assertThat(q2).isEqualTo("from 'Dtos' where vector.search(embedding.f32_i8(EmbeddingField), $p0, 0.65, null)");

                String q3 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withEmbedding("EmbeddingField").targetQuantization(VectorEmbeddingType.INT8), f -> f.byBase64("abcd=="), 0.75f)
                        .toString();
                assertThat(q3).isEqualTo("from 'Dtos' where vector.search(embedding.f32_i8(EmbeddingField), $p0, 0.75, null)");

                String q4 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withText("TextField"), f -> f.byText("abc"))
                        .toString();
                assertThat(q4).isEqualTo("from 'Dtos' where vector.search(embedding.text(TextField), $p0)");

                String q5 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withBase64("Base64Field", VectorEmbeddingType.BINARY), f -> f.byBase64("ddddd=="), 0.85f)
                        .toString();
                assertThat(q5).isEqualTo("from 'Dtos' where vector.search(embedding.i1(Base64Field), $p0, 0.85, null)");

                String q6 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withBase64("Base64Field", VectorEmbeddingType.INT8), f -> f.byEmbedding(new Float[]{0.2f, 0.3f}))
                        .toString();
                assertThat(q6).isEqualTo("from 'Dtos' where vector.search(embedding.i8(Base64Field), $p0)");

                String q7 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withBase64(Dto::getEmbeddingBase64), f -> f.byBase64("abcd=="))
                        .toString();
                assertThat(q7).isEqualTo("from 'Dtos' where vector.search(embeddingBase64, $p0)");

                String q8 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withBase64(Dto::getEmbeddingBase64), f -> f.byBase64("abcd=="), null, 25, true)
                        .toString();
                assertThat(q8).isEqualTo("from 'Dtos' where exact(vector.search(embeddingBase64, $p0, null, 25))");

                String q9 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withText(Dto::getTextualValue), f -> f.byTexts(Arrays.asList("a", "b")))
                        .toString();
                assertThat(q9).isEqualTo("from 'Dtos' where vector.search(embedding.text(textualValue), $p0)");

                String q10 = session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withBase64("Base64Field"), f -> f.byBase64(Arrays.asList("abcd==", "efgh==")))
                        .toString();
                assertThat(q10).isEqualTo("from 'Dtos' where vector.search(Base64Field, $p0)");

                assertThatThrownBy(() -> session.advanced().documentQuery(Dto.class)
                        .vectorSearch(x -> x.withField("VectorField"), f -> { })
                        .toString())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Cannot use VectorSearch without text(s), document id or embedding(s).");
            }
        }
    }

    @Test
    public void testEmbeddingDimensionsMismatchExceptionWithExplicitlySetDimensions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Dto dto = new Dto();
                dto.setEmbeddingSingles(new Float[]{0.1f, 0.2f, 0.3f});
                session.store(dto);
                session.saveChanges();
            }

            new IndexWithSetDimensions().execute(store);

            IndexErrors[] indexErrors = waitForIndexingErrors(store, Duration.ofSeconds(15));
            assertThat(indexErrors).hasSize(1);
            assertThat(indexErrors[0].getErrors()[0].getError())
                    .contains("Attempted to index embedding with 3 dimensions, but field Singles already contains indexed embedding with 256 dimensions, or was explicitly configured for embeddings with 256 dimensions.");
        }
    }

    @Test
    public void testEmbeddingDimensionsMismatchExceptionWithExplicitlySetDimensionsForInt8() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Dto dto = new Dto();
                dto.setEmbeddingSingles(new Float[]{0.1f, 0.2f, 0.3f, 0.7f});
                session.store(dto);
                session.saveChanges();
            }

            IndexWithSetDimensionsInt8 index = new IndexWithSetDimensionsInt8();
            IndexDefinition definition = index.createIndexDefinition();
            assertThat(definition.getFields().get("Sbytes").getVector().getDimensions()).isEqualTo(22);
            assertThat(definition.getFields().get("Sbytes").getVector().getSourceEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
            assertThat(definition.getFields().get("Sbytes").getVector().getDestinationEmbeddingType()).isEqualTo(VectorEmbeddingType.INT8);

            index.execute(store);

            IndexErrors[] indexErrors = waitForIndexingErrors(store, Duration.ofSeconds(15));
            assertThat(indexErrors).hasSize(1);
            assertThat(indexErrors[0].getErrors()[0].getError())
                    .contains("Attempted to index embedding with 4 dimensions, but field Sbytes already contains indexed embedding with 22 dimensions, or was explicitly configured for embeddings with 22 dimensions.");
        }
    }

    @Test
    public void vectorOptionsFactoryValidation() {
        assertThatThrownBy(InvalidQuantizationIndex::new)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot change the quantization of the already quantizied vector.");

        assertThatThrownBy(() -> new InvalidDimensionsIndex().createIndexDefinition())
                .isInstanceOf(IndexCompilationException.class)
                .hasRootCauseMessage("Number of vector dimensions has to be positive.");
    }

    @Test
    public void testAutoIndexCreationWithExactSearch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Dto dto = new Dto();
                dto.setEmbeddingSingles(new Float[]{0.2f, 0.3f});
                session.store(dto);
                session.saveChanges();

                session.query(Dto.class)
                        .vectorSearch(x -> x.withEmbedding(Dto::getEmbeddingSingles), f -> f.byEmbedding(new Float[]{0.2f, 0.3f}), null, null, true)
                        .toList();
            }

            IndexDefinition[] indexDefinitions = store.maintenance().send(new GetIndexesOperation(0, 10));
            assertThat(indexDefinitions).hasSize(1);
            assertThat(indexDefinitions[0].getName()).isEqualTo("Auto/Dtos/ByVector.search(embeddingSingles)");

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            AutoIndexDefinition autoIndex = record.getAutoIndexes().get("Auto/Dtos/ByVector.search(embeddingSingles)");
            assertThat(autoIndex).isNotNull();
            AutoIndexFieldOptions field = autoIndex.getMapFields().values().iterator().next();
            assertThat(field.getVector()).isNotNull();
            assertThat(field.getVector().getSourceEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
            assertThat(field.getVector().getDestinationEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
        }
    }

    @Test
    public void testDefaultVectorEmbeddingType() {
        assertThat(VectorEmbeddingType.values()[0]).isEqualTo(VectorEmbeddingType.SINGLE);
    }

    public static class Dto {
        private String embeddingBase64;
        private Float[] embeddingSingles;
        private String textualValue;

        public String getEmbeddingBase64() {
            return embeddingBase64;
        }

        public void setEmbeddingBase64(String embeddingBase64) {
            this.embeddingBase64 = embeddingBase64;
        }

        public Float[] getEmbeddingSingles() {
            return embeddingSingles;
        }

        public void setEmbeddingSingles(Float[] embeddingSingles) {
            this.embeddingSingles = embeddingSingles;
        }

        public String getTextualValue() {
            return textualValue;
        }

        public void setTextualValue(String textualValue) {
            this.textualValue = textualValue;
        }
    }

    public static class IndexWithSetDimensions extends AbstractIndexCreationTask {
        public IndexWithSetDimensions() {
            map = "from dto in docs.Dtos select new { Singles = CreateVector(dto.embeddingSingles) }";
            vector("Singles", f -> f.dimensions(256));
        }
    }

    public static class IndexWithSetDimensionsInt8 extends AbstractIndexCreationTask {
        public IndexWithSetDimensionsInt8() {
            map = "from dto in docs.Dtos select new { Sbytes = CreateVector(dto.embeddingSingles) }";
            vector("Sbytes", f -> f.dimensions(22).destinationEmbedding(VectorEmbeddingType.INT8));
        }
    }

    public static class InvalidQuantizationIndex extends AbstractIndexCreationTask {
        public InvalidQuantizationIndex() {
            map = "from dto in docs.Dtos select new { Sbytes = CreateVector(dto.embeddingSingles) }";
            vector("Sbytes", f -> f.sourceEmbedding(VectorEmbeddingType.INT8).destinationEmbedding(VectorEmbeddingType.SINGLE));
        }
    }

    public static class InvalidDimensionsIndex extends AbstractIndexCreationTask {
        public InvalidDimensionsIndex() {
            map = "from dto in docs.Dtos select new { Singles = CreateVector(dto.embeddingSingles) }";
            vector("Singles", f -> f.dimensions(0));
        }
    }
}
