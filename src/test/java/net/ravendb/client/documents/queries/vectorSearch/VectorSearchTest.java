package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.operations.indexes.GetIndexesOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.indexes.IndexType;
import com.google.common.collect.Sets;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.1")
public class VectorSearchTest extends RemoteTestBase {

    public static class User {
        private String name;
        private int age;
        private Float[] embedding;
        
        public  User (String name, Float[] embedding) {
            this.name = name;
            this.embedding = embedding;
        }

        public User(){}

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

        public Float[] getEmbedding() {return this.embedding;}
        public void setEmbedding(Float[] embedding) {this.embedding = embedding;}
    }

    @Test
    public void shouldReturnDocsWithVectorSearchAPI(){
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User("John", new Float[]{0.1f, 0.2f, 0.3f}), "users/1");
                session.store(new User("Alice", new Float[]{0.15f, 0.25f, 0.35f}), "users/2");
                session.store(new User("Bob", new Float[]{0.4f, 0.5f, 0.6f}), "users/3");
                session.store(new User("Charlie", new Float[]{0.9f, 1.1f, 2.0f}), "users/4");
                session.store(new User("Diana", new Float[]{3.0f, 2.5f, 4.0f}), "users/5");
                session.store(new User("Eve", new Float[]{5.2f, 6.0f, 7.1f}), "users/6");
                session.store(new User("Frank", new Float[]{8.0f, 9.3f, 10.0f}), "users/7");
                session.saveChanges();
            }
            try (IDocumentSession session = store.openSession()) {
                List<User> results = session.query(User.class).vectorSearch(
                        x -> x.withText("name"),
                        vf -> vf.byText("Ali")
                ).toList();

                assertThat(results).hasSize(7);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldReturnDocsWithVectorSearchAsRQL(){
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User("John", new Float[]{0.1f, 0.2f, 0.3f}), "users/1");
                session.store(new User("Alice", new Float[]{0.15f, 0.25f, 0.35f}), "users/2");
                session.store(new User("Bob", new Float[]{0.4f, 0.5f, 0.6f}), "users/3");
                session.store(new User("Charlie", new Float[]{0.9f, 1.1f, 2.0f}), "users/4");
                session.store(new User("Diana", new Float[]{3.0f, 2.5f, 4.0f}), "users/5");
                session.store(new User("Eve", new Float[]{5.2f, 6.0f, 7.1f}), "users/6");
                session.store(new User("Frank", new Float[]{8.0f, 9.3f, 10.0f}), "users/7");
                session.saveChanges();
            }
            try (IDocumentSession session = store.openSession()) {
                String vectorQuery =
                        "from Users as u " +
                                "where vector.search(u.embedding, $p0) " +
                                "select u.name";

                float[] searchVector = new float[]{0.1f, 0.2f, 0.25f};
                List<User> results = session.advanced()
                        .rawQuery(User.class, vectorQuery)
                        .addParameter("p0", searchVector)
                        .toList();

                assertThat(results).hasSize(7);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithInt8QuantizedEmbeddingField() {
        Float[] arr = new Float[]{2.5f, 3.3f};
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                                .vectorSearch(
                                        x -> x.withEmbedding("EmbeddingField", VectorEmbeddingType.INT8).targetQuantization(VectorEmbeddingType.INT8),
                                        vf -> vf.byEmbedding(arr),
                                        0.65f,
                                        12,
                                        null
                                ).toString();

                assertThat(query)
                        .isEqualTo("from 'Users' where vector.search(embedding.i8(EmbeddingField), $p0, 0.65, 12)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithTextEmbeddingUsingAiTask() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withText("VectorField").usingTask("id-for-task-open-ai"),
                                vf -> vf.byText("aaaa")
                        ).toString();

                assertEquals(
                        "from 'Users' where vector.search(embedding.text(VectorField, ai.task('id-for-task-open-ai')), $p0)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForBasicVectorSearchWithNumericEmbeddingValues() {
        Float[] arr = new Float[]{2.5f, 3.3f};

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withField("VectorField"),
                                vf -> vf.byEmbedding(arr)
                        )
                        .toString();

                assertEquals(
                        "from 'Users' where vector.search(VectorField, $p0)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithBase64EncodedEmbedding() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withField("VectorField"),
                                vf -> vf.byBase64("aaaa==")
                        )
                        .toString();

                assertEquals(
                        "from 'Users' where vector.search(VectorField, $p0)",
                        query
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldAndInt8Quantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("EmbeddingSingles").targetQuantization(VectorEmbeddingType.INT8),
                            vf -> vf.byText("aaaa")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i8(EmbeddingSingles), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchUsingPropertySelectorForEmbeddingField() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingSingles"),
                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f})
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingSingles, $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorAndExplicitInt8Quantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingSBytes", VectorEmbeddingType.INT8),
                            vf -> vf.byEmbedding(new Integer[]{1, 2, 3}),
                            0.75f, null, null
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorAndExplicitBinaryQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingBinary", VectorEmbeddingType.BINARY),
                            vf -> vf.byEmbedding(new Integer[]{0, 1, 0, 1})
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.i1(EmbeddingBinary), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithPropertySelectorForTextFieldConversion() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue"),
                            vf -> vf.byText("search text")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text(TextualValue), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldUsingNamedAiTask() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                           x-> x.withText("TextualValue")
                                    .usingTask("taskId-123"),
                            vf -> vf.byText("query text")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text(TextualValue, ai.task('taskId-123')), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithBase64FieldUsingPropertySelector() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withBase64("EmbeddingBase64", null),
                            vf -> vf.byBase64("aGVsbG8=")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(EmbeddingBase64, $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithSingleToInt8ConversionQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingSingles")
                                    .targetQuantization(VectorEmbeddingType.INT8),
                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f})
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.f32_i8(EmbeddingSingles), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithSingleToBinaryConversionQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x. withEmbedding("EmbeddingSingles")
                                    .targetQuantization(VectorEmbeddingType.BINARY),
                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f})
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.f32_i1(EmbeddingSingles), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithTextFieldAndInt8TargetQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue")
                                    .targetQuantization(VectorEmbeddingType.INT8),
                            vf -> vf.byText("query text")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i8(TextualValue), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithTextAiTaskAndBinaryQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue")
                                    .usingTask("openai-embeddings")
                                    .targetQuantization(VectorEmbeddingType.BINARY),
                            vf -> vf.byText("query text")
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.text_i1(TextualValue, ai.task('openai-embeddings')), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithWithFieldMethodAndPropertySelector() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withField("EmbeddingSingles"),
                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            null, 20, null
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithExactMatchingParameter() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withField("VectorField"),
                            vf -> vf.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            null, null, true
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(VectorField, $p0))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithSimilarityCandidatesAndExactParameters() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withField("VectorField"),
                            vf -> vf.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
                            0.75f, 50, true
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithExactParameterAndEmbeddingField() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingSingles"),
                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
                            null,null,true
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(EmbeddingSingles, $p0))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithExactParameterAndTextEmbeddingWithSimilarity() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue"),
                            vf -> vf.byText("query text"),
                            0.8f, null, true
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleTextQueriesAsInput() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue"),
                            vf -> vf.byTexts(new String[]{"first query", "second query"}),
                            0.75f, null, null
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleEmbeddingVectorsAsInput() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withField("EmbeddingSingles"),
                            vf -> vf.byEmbeddings(new Float[][]{
                                    {0.1f, 0.2f, 0.3f},
                                    {0.4f, 0.5f, 0.6f}
                            }),
                            null, 30, null
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

    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleEmbeddingsAndInt8Quantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {

            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withEmbedding("EmbeddingSBytes", VectorEmbeddingType.INT8)
                                    .targetQuantization(VectorEmbeddingType.INT8),
                            vf -> vf.byEmbeddings(new Integer[][]{
                                    {1, 2, 3},
                                    {4, 5, 6}
                            })
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where vector.search(embedding.i8(EmbeddingSBytes), $p0)",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithMultipleTextsAiTaskAndBinaryQuantization() {
        try (IDocumentStore store = getDocumentStore();
             IDocumentSession session = store.openSession()) {
            String query = session.query(User.class)
                    .vectorSearch(
                            x-> x.withText("TextualValue")
                                    .usingTask("openai-embeddings")
                                    .targetQuantization(VectorEmbeddingType.BINARY),
                            vf -> vf.byTexts(new String[]{"query one", "query two", "query three"}),
                            null,null,true
                    )
                    .toString();

            assertEquals(
                    "from 'Users' where exact(vector.search(embedding.text_i1(TextualValue, ai.task('openai-embeddings')), $p0))",
                    query
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
            assertThat(indexDef.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE)).isEqualTo("Corax");

            IndexFieldOptions vectorField = indexDef.getFields().get("FirstName");
            FieldVectorOptions v = vectorField.getVector();

            assertThat(v.getSourceEmbeddingType()).isEqualTo(VectorEmbeddingType.TEXT);
            assertThat(v.getDestinationEmbeddingType()).isEqualTo(VectorEmbeddingType.SINGLE);
            assertThat(v.getNumberOfEdges()).isEqualTo(23);
            assertThat(v.getNumberOfCandidatesForIndexing()).isEqualTo(20);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    @Test
    public void shouldGenerateRqlForVectorSearchUsingForDocumentWithTextField() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withText("TextualValue"),
                                vf -> vf.forDocument("dtos/456"),
                                null,null,null
                        )
                        .toString();
                assertThat(query).isEqualTo(
                        "from 'Users' where vector.search(embedding.text(TextualValue), embedding.forDoc($p0))"
                );
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchUsingForDocumentWithInt8Quantization() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withEmbedding("EmbeddingSBytes", VectorEmbeddingType.INT8),
                                factory -> factory.forDocument("dtos/int8-test")
                        )
                        .toString();

                assertThat(query).isEqualTo(
                        "from 'Users' where vector.search(embedding.i8(EmbeddingSBytes), embedding.forDoc($p0))"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchUsingForDocumentWithTextFieldAndAITask() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withText("TextualValue")
                                        .usingTask("openai-task"),
                                vf -> vf.forDocument("dtos/789")
                        )
                        .toString();

                assertThat(query).isEqualTo(
                        "from 'Users' where vector.search(embedding.text(TextualValue, ai.task('openai-task')), embedding.forDoc($p0))"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldAndByTextWithGenerationTaskIdentifier(){
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withField("VectorField"),
                                vf -> vf.byText("car", "localaitask")
                        )
                        .toString();
                assertThat(query).isEqualTo("from 'Users' where vector.search(VectorField, embedding.text($p0, ai.task('localaitask')))");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchWithFieldAndByTextsWithGenerationTaskIdentifier(){
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withField("VectorField"),
                                vf -> vf.byTexts(new String[]{"car", "bus"}, "localaitask")
                        )
                        .toString();
                assertThat(query).isEqualTo("from 'Users' where vector.search(VectorField, embedding.text($p0, ai.task('localaitask')))");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSetVectorFieldStrings() {
        TestIndexDefinitionBuilder builder = new TestIndexDefinitionBuilder("TestIndex");
        Map<String, FieldVectorOptions> vectorFields = new HashMap<>();
        vectorFields.put("Embedding", new FieldVectorOptions());
        builder.setVectorFieldStrings(vectorFields);

        assertEquals(vectorFields, builder.getVectorFieldStrings());
    }

    @Test
    public void shouldGenerateRqlForVectorSearchUsingForDocumentWithSimilarityAndCandidates() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withEmbedding("EmbeddingSingles"),
                                factory -> factory.forDocument("dtos/full-options"),
                                0.75f, 100, null
                        )
                        .toString();

                assertThat(query).isEqualTo(
                        "from 'Users' where vector.search(EmbeddingSingles, embedding.forDoc($p0), 0.75, 100)"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldGenerateRqlForVectorSearchUsingForDocumentWithNumberOfCandidates() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                String query = session.query(User.class)
                        .vectorSearch(
                                x-> x.withField("VectorField"),
                                factory -> factory.forDocument("dtos/candidates-test"),
                                null, 50, null
                        )
                        .toString();

                assertThat(query).isEqualTo(
                        "from 'Users' where vector.search(VectorField, embedding.forDoc($p0), null, 50)"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Vector Search tests with String as parameter not a lambda
    //
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsString() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//
//        try (IDocumentStore store = getDocumentStore();
//             IDocumentSession session = store.openSession()) {
//
//            String query = session.query(User.class)
//                    .vectorSearch(
//                            "VectorField",
//                            vf -> vf.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
//                            null // options
//                    )
//                    .toString();
//
//            assertEquals(
//                    "from 'Users' where vector.search(VectorField, $p0)",
//                    query
//            );
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

//
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndOptions() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//        IVectorOptions options = new IVectorOptions();
//        options.setSimilarity(0.75);
//        options.setNumberOfCandidates(20);
//
//        try (IDocumentStore store = getDocumentStore();
//             IDocumentSession session = store.openSession()) {
//            String query = session.query(User.class)
//                    .vectorSearch(
//                            "EmbeddingSingles",
//                            vf -> vf.byEmbedding(new Float[]{0.1f, 0.2f, 0.3f}),
//                            options
//                    )
//                    .toString();
//
//            assertEquals(
//                    "from 'Users' where vector.search(EmbeddingSingles, $p0, 0.75, 20)",
//                    query
//            );
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    //
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndExactParameter() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//        IVectorOptions options = new IVectorOptions();
//        options.setIsExact(true);
//
//        try (IDocumentStore store = getDocumentStore();
//             IDocumentSession session = store.openSession()) {
//            String query = session.query(User.class)
//                    .vectorSearch(
//                            "VectorField",
//                            vf -> vf.byEmbedding(new Float[]{0.3f, 0.4f, 0.5f}),
//                            options
//                    )
//                    .toString();
//
//            assertEquals(
//                    "from 'Users' where exact(vector.search(VectorField, $p0))",
//                    query
//            );
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndMultipleEmbeddings() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//        IVectorOptions options = new IVectorOptions();
//        options.setSimilarity(0.8);
//
//        try (IDocumentStore store = getDocumentStore();
//             IDocumentSession session = store.openSession()) {
//            String query = session.query(User.class)
//                    .vectorSearch(
//                            "EmbeddingSingles",
//                            factory -> factory.byEmbeddings(new Number[][] {
//                                    new Double[] { 0.1, 0.2, 0.3 },
//                                    new Double[] { 0.4, 0.5, 0.6 }
//                            }),
//                            options
//                    )
//                    .toString();
//
//            assertEquals(
//                    "from 'Users' where vector.search(EmbeddingSingles, $p0, 0.8, null)",
//                    query
//            );
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndByTextFactory() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//
//        try (IDocumentStore store = getDocumentStore();
//             IDocumentSession session = store.openSession()) {
//
//            String query = session.query(User.class)
//                    .vectorSearch(
//                            "TextualValue",
//                            vf -> vf.byText("query text"),
//                            null
//                    )
//                    .toString();
//
//            assertEquals(
//                    "from 'Users' where vector.search(TextualValue, $p0)",
//                    query
//            );
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    @Test
//    public void shouldGenerateRqlForVectorSearchWithFieldNameAsStringAndForDocumentFactory() {
//        VectorEmbeddingFieldValueFactory valueFactory = new VectorEmbeddingFieldValueFactory();
//
//        try (IDocumentStore store = getDocumentStore()) {
//            try (IDocumentSession session = store.openSession()) {
//                String query = session.query(User.class)
//                        .vectorSearch("TextualValue", vf -> vf.forDocument("users/1"), null)
//                        .toString();
//
//                assertThat(query)
//                        .isEqualTo("from 'Users' where vector.search(TextualValue, embedding.forDoc($p0))");
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
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
        FieldVectorOptions vectorOptions = new FieldVectorOptions();
        vectorOptions.setNumberOfEdges(23);
        vectorOptions.setNumberOfCandidatesForIndexing(20);
        vectorOptions.setSourceEmbeddingType(VectorEmbeddingType.TEXT);
        vectorOptions.setDestinationEmbeddingType(VectorEmbeddingType.SINGLE);
        fieldOptions.setVector(vectorOptions);

        indexDefinition.getFields().put("FirstName", fieldOptions);

        indexDefinition.getConfiguration().put(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE, "Corax");

        PutIndexesOperation putIndexesOperation = new PutIndexesOperation(indexDefinition);
        PutIndexResult[] results = store.maintenance().send(putIndexesOperation);
        assertThat(results).hasSize(1);
        assertThat(results[0].getIndex()).isEqualTo(indexDefinition.getName());
        assertThat(fieldOptions.getVector()).isNotNull();
    }

    private class TestIndexDefinition extends IndexDefinition {}

    class TestIndexDefinitionBuilder extends AbstractIndexDefinitionBuilder<TestIndexDefinition> {
        public TestIndexDefinitionBuilder(String indexName) {
            super(indexName);
        }
        @Override
        protected TestIndexDefinition newIndexDefinition() {
            return new TestIndexDefinition();
        }
        @Override
        protected void toIndexDefinition(TestIndexDefinition indexDefinition, net.ravendb.client.documents.conventions.DocumentConventions conventions) {}
        public Map<String, FieldVectorOptions> getVectorFieldStrings() {
            return super.getVectorFieldStrings();
        }
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
            FieldVectorOptions fieldVectorOptions = new FieldVectorOptions();
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