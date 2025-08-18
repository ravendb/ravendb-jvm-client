package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.operations.indexes.GetIndexesOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.VectorEmbeddingFieldValueFactory;
import net.ravendb.client.documents.indexes.IndexType;
import com.google.common.collect.Sets;
import net.ravendb.client.infrastructure.EnableOn70Server;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;


public class VectorSearchTest extends RemoteTestBase {

    public static class User {
        private String name;
        private int age;
        private String embeddingBase64;
        private double[] embeddingSingles;
        private byte[] embeddingSBytes;
        private byte[] embeddingBinary;
        private String textualValue;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
        public String getEmbeddingBase64() {
            return embeddingBase64;
        }

        public void setEmbeddingBase64(String embeddingBase64) {
            this.embeddingBase64 = embeddingBase64;
        }

        public double[] getEmbeddingSingles() {
            return embeddingSingles;
        }

        public void setEmbeddingSingles(double[] embeddingSingles) {
            this.embeddingSingles = embeddingSingles;
        }

        public byte[] getEmbeddingSBytes() {
            return embeddingSBytes;
        }

        public void setEmbeddingSBytes(byte[] embeddingSBytes) {
            this.embeddingSBytes = embeddingSBytes;
        }

        public byte[] getEmbeddingBinary() {
            return embeddingBinary;
        }

        public void setEmbeddingBinary(byte[] embeddingBinary) {
            this.embeddingBinary = embeddingBinary;
        }

        public String getTextualValue() {
            return textualValue;
        }

        public void setTextualValue(String textualValue) {
            this.textualValue = textualValue;
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithInt8QuantizedEmbeddingField() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        Float[] arr = new Float[]{2.5f, 3.3f};
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.65);
        options.setNumberOfCandidates(12);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                                .vectorSearch(
                                        vectorFieldFactory.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8).targetQuantization(VectorEmbeddingType.INT8).getFieldName(),
                                        ()-> valueFactory.byEmbedding(arr),
                                        options
                                ).toString();

                assertThat(query)
                        .isEqualTo("from 'Users' where vector.search(embedding.i8(EmbeddingField), $p0, 0.65, 12)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithTextEmbeddingUsingAiTask() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                vectorFieldFactory.withText("VectorField").usingTask("id-for-task-open-ai").getFieldName(),
                                ()-> valueFactory.byText("aaaa"),
                                null
                        ).toString();

                assertEquals(
                        "from 'Users' where vector.search(embedding.text(VectorField, ai.task('id-for-task-open-ai')), $p0, null, null)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForBasicVectorSearchWithNumericEmbeddingValues() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        Float[] arr = new Float[]{2.5f, 3.3f};

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                vectorFieldFactory.withField("VectorField").getFieldName(),
                                () -> valueFactory.byEmbedding(arr),
                                null
                        )
                        .toString();

                assertEquals(
                        "from 'Users' where vector.search(VectorField, $p0, null, null)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithBase64EncodedEmbedding() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                vectorFieldFactory.withField("VectorField").getFieldName(),
                                ()-> valueFactory.byBase64("aaaa=="),
                                null
                        )
                        .toString();

                assertEquals(
                        "from 'Users' where vector.search(VectorField, $p0, null, null)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldAndInt8Quantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("EmbeddingSingles").targetQuantization(VectorEmbeddingType.INT8).getFieldName(),
                            () -> valueFactory.byText("aaaa"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i8(EmbeddingSingles), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchUsingPropertySelectorForEmbeddingField() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSingles", null).getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorAndExplicitInt8Quantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.75);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSBytes", VectorEmbeddingType.INT8).getFieldName(),
                            () -> valueFactory.byEmbedding(new Integer[]{1, 2, 3}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.i8(EmbeddingSBytes), $p0, 0.75, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorAndExplicitBinaryQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingBinary", VectorEmbeddingType.BINARY).getFieldName(),
                            ()-> valueFactory.byEmbedding(new Integer[]{0, 1, 0, 1}),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.i1(EmbeddingBinary), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorForTextFieldConversion() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue").getFieldName(),
                            () -> valueFactory.byText("search text"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text(TextualValue), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldUsingNamedAiTask() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue")
                                    .usingTask("taskId-123")
                                    .getFieldName(),
                            () -> valueFactory.byText("query text"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text(TextualValue, ai.task('taskId-123')), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithBase64FieldUsingPropertySelector() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withBase64("EmbeddingBase64", null).getFieldName(),
                            ()-> valueFactory.byBase64("aGVsbG8="),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingBase64, $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithSingleToInt8ConversionQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSingles", null)
                                    .targetQuantization(VectorEmbeddingType.INT8)
                                    .getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.f32_i8(EmbeddingSingles), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithSingleToBinaryConversionQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSingles", null)
                                    .targetQuantization(VectorEmbeddingType.BINARY)
                                    .getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.f32_i1(EmbeddingSingles), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldAndInt8TargetQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue")
                                    .targetQuantization(VectorEmbeddingType.INT8)
                                    .getFieldName(),
                            () -> valueFactory.byText("query text"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i8(TextualValue), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithTextAiTaskAndBinaryQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue")
                                    .usingTask("openai-embeddings")
                                    .targetQuantization(VectorEmbeddingType.BINARY)
                                    .getFieldName(),
                            () -> valueFactory.byText("query text"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i1(TextualValue, ai.task('openai-embeddings')), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithWithFieldMethodAndPropertySelector() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setNumberOfCandidates(20);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withField("EmbeddingSingles").getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0, null, 20)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithExactMatchingParameter() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withField("VectorField").getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(VectorField, $p0, null, null))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithSimilarityCandidatesAndExactParameters() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.75);
        options.setNumberOfCandidates(50);
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withField("VectorField").getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(VectorField, $p0, 0.75, 50))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithExactParameterAndEmbeddingField() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSingles",null).getFieldName(),
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(EmbeddingSingles, $p0, null, null))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithExactParameterAndTextEmbeddingWithSimilarity() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.8);
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue").getFieldName(),
                            () -> valueFactory.byText("query text"),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(embedding.text(TextualValue), $p0, 0.8, null))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleTextQueriesAsInput() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.75);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue").getFieldName(),
                            () -> valueFactory.byTexts(new String[]{"first query", "second query"}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text(TextualValue), $p0, 0.75, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleEmbeddingVectorsAsInput() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setNumberOfCandidates(30);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withField("EmbeddingSingles").getFieldName(),
                            () -> valueFactory.byEmbeddings(new Float[][]{
                                    {0.1f, 0.2f, 0.3f},
                                    {0.4f, 0.5f, 0.6f}
                            }),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0, null, 30)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleEmbeddingsAndInt8Quantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withEmbedding("EmbeddingSBytes", VectorEmbeddingType.INT8)
                                    .targetQuantization(VectorEmbeddingType.INT8)
                                    .getFieldName(),
                            () -> valueFactory.byEmbeddings(new Integer[][]{
                                    {1, 2, 3},
                                    {4, 5, 6}
                            }),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.i8(EmbeddingSBytes), $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleTextsAiTaskAndBinaryQuantization() {
        VectorEmbeddingFieldFactory vectorFieldFactory = new VectorEmbeddingFieldFactory();
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            vectorFieldFactory.withText("TextualValue")
                                    .usingTask("openai-embeddings")
                                    .targetQuantization(VectorEmbeddingType.BINARY)
                                    .getFieldName(),
                            () -> valueFactory.byTexts(new String[]{"query one", "query two", "query three"}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(embedding.text_i1(TextualValue, ai.task('openai-embeddings')), $p0, null, null))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldNameAsString() {
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            "VectorField",
                            () -> valueFactory.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            null // options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(VectorField, $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndOptions() {
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.75);
        options.setNumberOfCandidates(20);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            "EmbeddingSingles",
                            () -> valueFactory.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0, 0.75, 20)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndExactParameter() {
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setIsExact(true);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            "VectorField",
                            () -> valueFactory.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(VectorField, $p0, null, null))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndMultipleEmbeddings() {
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
        IVectorOptions options = new IVectorOptions();
        options.setSimilarity(0.8);

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            "EmbeddingSingles",
                            () -> valueFactory.byEmbeddings(new Float[][]{
                                    {0.1f, 0.2f, 0.3f},
                                    {0.4f, 0.5f, 0.6f}
                            }),
                            options
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0, 0.8, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndByTextFactory() {
        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();

        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            "TextualValue",
                            () -> valueFactory.byText("query text"),
                            null
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(TextualValue, $p0, null, null)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldCreateIndexDefinitionWithVectorSearchFieldAndProperConfiguration() {
        try (IDocumentStore store = getDocumentStore()) {
            setupIndexDefinition(store);
            IndexDefinition[] indexDefinitions = store.maintenance()
                    .send(new GetIndexesOperation(0, 10));

            assertThat(indexDefinitions).hasSizeGreaterThan(0);

            IndexDefinition indexDef = indexDefinitions[0];

            assertThat(indexDef.getName()).isEqualTo("Users/ByEmbeddingSingles");
            assertThat(indexDef.getType().name()).isEqualTo("MAP");
            assertThat(indexDef.getConfiguration().get("Indexing.Static.SearchEngineType")).isEqualTo("Corax");

            IndexFieldOptions vectorField = indexDef.getFields().get("FirstName");
            VectorFieldOptions v = vectorField.getVector();

            assertThat(v.getSourceEmbeddingType()).isEqualTo(VectorEmbeddingType.TEXT);
            assertThat(v.getDestinationEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
            assertThat(v.getNumberOfEdges()).isEqualTo(23);
            assertThat(v.getNumberOfCandidatesForIndexing()).isEqualTo(20);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOn70Server
    @Test
    public void shouldCreateIndexWithVectorSearchConfigurationUsingClassBasedDefinition() {
        try (IDocumentStore store = getDocumentStore()) {
            setupIndexClass(store);
            IndexDefinition[] indexDefinitions = store.maintenance()
                    .send(new GetIndexesOperation(0, 10));

            assertThat(indexDefinitions).hasSize(1);

            IndexDefinition indexDefinition = indexDefinitions[0];
            assertThat(indexDefinition.getName()).isEqualTo("Users/ByEmbeddingSingles");
            assertThat(indexDefinition.getType()).isEqualTo(IndexType.JAVA_SCRIPT_MAP);
            assertThat(indexDefinition.getConfiguration().get("Indexing.Static.SearchEngineType"))
                    .isEqualTo("Corax");

            IndexFieldOptions vectorField = indexDefinition.getFields().get("vectorField");
            assertThat(vectorField.getVector().getSourceEmbeddingType()).isEqualTo(VectorEmbeddingType.TEXT);
            assertThat(vectorField.getVector().getDestinationEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
            assertThat(vectorField.getVector().getNumberOfEdges()).isEqualTo(33);
            assertThat(vectorField.getVector().getNumberOfCandidatesForIndexing()).isEqualTo(43);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setupIndexDefinition(IDocumentStore store) {
        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName("Users/ByEmbeddingSingles");

        indexDefinition.setMaps(Collections.singleton(
                "from doc in docs.Users " +
                        "select new { " +
                        "    doc.EmbeddingSingles, " +
                        "    EmbeddingSinglesVector = CreateVector(doc.EmbeddingSingles) " +
                        "}"
        ));


        IndexFieldOptions fieldOptions = new IndexFieldOptions();
        VectorFieldOptions vectorOptions = new VectorFieldOptions();
        vectorOptions.setNumberOfEdges(23);
        vectorOptions.setNumberOfCandidatesForIndexing(20);
        vectorOptions.setSourceEmbeddingType(VectorEmbeddingType.TEXT);
        vectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.SINGLE);
        fieldOptions.setVector(vectorOptions);

        indexDefinition.getFields().put("FirstName", fieldOptions);

        indexDefinition.getConfiguration().put("Indexing.Static.SearchEngineType", "Corax");

        PutIndexesOperation putIndexesOperation = new PutIndexesOperation(indexDefinition);
        PutIndexResult[] results = store.maintenance().send(putIndexesOperation);
        assertThat(results).hasSize(1);
        assertThat(results[0].getIndex()).isEqualTo(indexDefinition.getName());
    }

    private class Users_ByEmbeddingSingles extends AbstractJavaScriptIndexCreationTask {
        public Users_ByEmbeddingSingles() {
            super();
            setMaps(Sets.newHashSet(
                    "map('Users', function (doc) { " +
                            "    return { " +
                            "        EmbeddingSingles: doc.EmbeddingSingles, " +
                            "        EmbeddingSinglesVector: createVector(doc.EmbeddingSingles) " +
                            "    }; " +
                            "})"
            ));

            IndexFieldOptions vectorOptions = new IndexFieldOptions();
            VectorFieldOptions fieldVectorOptions = new VectorFieldOptions();
            fieldVectorOptions.setNumberOfEdges(33);
            fieldVectorOptions.setNumberOfCandidatesForIndexing(43);
            fieldVectorOptions.setSourceEmbeddingType(VectorEmbeddingType.TEXT);
            fieldVectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.SINGLE);
            vectorOptions.setVector(fieldVectorOptions);
            this.getFields().put("vectorField", vectorOptions);
            this.getConfiguration().put("Indexing.Static.SearchEngineType", "Corax");
        }
    }

    private void setupIndexClass(IDocumentStore store) {
        Users_ByEmbeddingSingles dtoIndex = new Users_ByEmbeddingSingles();
        dtoIndex.execute(store);
    }
}