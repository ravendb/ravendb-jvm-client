package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.*;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperationResult;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTask;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingsGenerationEtlTest extends RemoteTestBase {

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canAddEmbeddingsGenerationTaskWithPathBasedConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putEmbeddingsConnectionString(store);

            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Products Embeddings Path-Based");
            config.setCollection("Products");
            config.setConnectionStringName(csName);
            config.setIdentifier("products-embeddings-path-based");

            List<EmbeddingPathConfiguration> pathConfigs = new ArrayList<>();

            EmbeddingPathConfiguration descConfig = new EmbeddingPathConfiguration();
            descConfig.setPath("Description");
            descConfig.setChunkingOptions(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT_PARAGRAPHS, 256, 32));

            EmbeddingPathConfiguration detailsConfig = new EmbeddingPathConfiguration();
            detailsConfig.setPath("Details");
            detailsConfig.setChunkingOptions(new ChunkingOptions(ChunkingMethod.MARK_DOWN_SPLIT_PARAGRAPHS, 512, 64));

            pathConfigs.add(descConfig);
            pathConfigs.add(detailsConfig);

            config.setEmbeddingsPathConfigurations(pathConfigs);

            config.setChunkingOptionsForQuerying(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setQuantization(VectorEmbeddingType.INT8);

            AddEmbeddingsGenerationOperation operation = new AddEmbeddingsGenerationOperation(config);
            AddEmbeddingsGenerationOperationResult result = store.maintenance().send(operation);

            assertThat(result.getTaskId()).isGreaterThan(0);
            assertThat(result.getIdentifier()).isNotNull();
            assertThat(result.getIdentifier()).isEqualTo(config.getIdentifier());
        }

    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canAddEmbeddingsGenerationTaskWithScriptBasedConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putEmbeddingsConnectionString(store);

            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Articles Embeddings Script-Based");
            config.setCollection("Articles");
            config.setConnectionStringName(csName);
            config.setIdentifier("articles-embeddings-script-based");

            EmbeddingsTransformation transformation = new EmbeddingsTransformation();
            transformation.setScript(
                    "var title = this.Title || \"\";\n" +
                            "var body = this.Body || \"\";\n" +
                            "var combined = title + \"\\n\\n\" + body;\n" +
                            "\n" +
                            "embeddings.generate({\n" +
                            "    text: combined,\n" +
                            "    field: \"ContentEmbedding\"\n" +
                            "});"
            );

            ChunkingOptions scriptChunking = new ChunkingOptions(ChunkingMethod.MARK_DOWN_SPLIT_PARAGRAPHS, 512 ,64);
            transformation.setChunkingOptions(scriptChunking);
            config.setEmbeddingsTransformation(transformation);

            config.setChunkingOptionsForQuerying(
                    new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setQuantization(VectorEmbeddingType.SINGLE);

            AddEmbeddingsGenerationOperation operation = new AddEmbeddingsGenerationOperation(config);
            AddEmbeddingsGenerationOperationResult result = store.maintenance().send(operation);

            assertThat(result.getTaskId()).isGreaterThan(0);
            assertThat(result.getIdentifier()).isNotNull();
            assertThat(result.getIdentifier()).isEqualTo(config.getIdentifier());
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canUpdateEmbeddingsGenerationTask() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putEmbeddingsConnectionString(store);

            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Products Embeddings Update Test");
            config.setCollection("Products");
            config.setConnectionStringName(csName);

            EmbeddingPathConfiguration descConfig = new EmbeddingPathConfiguration();
            descConfig.setPath("Description");
            descConfig.setChunkingOptions(
                    new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setEmbeddingsPathConfigurations(Collections.singletonList(descConfig));

            config.setChunkingOptionsForQuerying(
                    new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256 ,0));

            config.setQuantization(VectorEmbeddingType.INT8);

            AddEmbeddingsGenerationOperation addOp = new AddEmbeddingsGenerationOperation(config);
            AddEmbeddingsGenerationOperationResult addResult = store.maintenance().send(addOp);
            long taskId = addResult.getTaskId();

            config.setQuantization(VectorEmbeddingType.SINGLE);
            config.setEmbeddingsCacheExpiration(Duration.ofDays(30));

            UpdateEmbeddingsGenerationOperation updateOp =
                    new UpdateEmbeddingsGenerationOperation(taskId, config);

            UpdateEtlOperationResult updateResult = store.maintenance().send(updateOp);

            assertThat(updateResult.getTaskId()).isEqualTo(taskId + 1);
            assertThat(updateResult.getRaftCommandIndex()).isGreaterThan(0);
        }
    }

    @Test
    public void validatesCacheExpirationSettings() throws Exception {
        try(IDocumentStore store = getDocumentStore()){
            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Test Cache Expiration");
            config.setCollection("Products");
            config.setIdentifier("test-cache");

            EmbeddingPathConfiguration descConfig = new EmbeddingPathConfiguration();
            descConfig.setPath("Description");
            descConfig.setChunkingOptions(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setEmbeddingsPathConfigurations(Collections.singletonList(descConfig));

            config.setChunkingOptionsForQuerying(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setEmbeddingsCacheExpiration(Duration.ofDays(60));
            config.setEmbeddingsCacheForQueryingExpiration(Duration.ofDays(7));

            ObjectMapper mapper = store.getConventions().getEntityMapper();

            JsonNode serialized = mapper.valueToTree(config);

            assertThat(serialized.get("EmbeddingsCacheExpiration")).isNotNull();
            assertThat(serialized.get("EmbeddingsCacheForQueryingExpiration")).isNotNull();
        }
    }

    @Test
    public void validatesTransformationNameMapping() {
        EmbeddingsGenerationConfiguration pathConfig = new EmbeddingsGenerationConfiguration();

        EmbeddingPathConfiguration descConfig = new EmbeddingPathConfiguration();
        descConfig.setPath("Description");
        descConfig.setChunkingOptions(
                new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

        pathConfig.setEmbeddingsPathConfigurations(Collections.singletonList(descConfig));

        assertThat(pathConfig.getTransformationName())
                .isEqualTo("embeddings-from-paths");

        EmbeddingsGenerationConfiguration scriptConfig = new EmbeddingsGenerationConfiguration();

        EmbeddingsTransformation transformation = new EmbeddingsTransformation();
        transformation.setScript("embeddings.generate({ text: this.Title });");
        transformation.setChunkingOptions(
                new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

        scriptConfig.setEmbeddingsTransformation(transformation);

        assertThat(scriptConfig.getTransformationName())
                .isEqualTo("embeddings-transform-script");
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canGetOngoingTaskInfo() throws Exception {
        try(IDocumentStore store = getDocumentStore()){
            String csName = putEmbeddingsConnectionString(store);

            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Products Embeddings Task Info Test");
            config.setCollection("Products");
            config.setConnectionStringName(csName);

            EmbeddingPathConfiguration descConfig = new EmbeddingPathConfiguration();
            descConfig.setPath("Description");
            descConfig.setChunkingOptions(
                    new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));
            config.setEmbeddingsPathConfigurations(Collections.singletonList(descConfig));

            config.setChunkingOptionsForQuerying(
                    new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));

            config.setQuantization(VectorEmbeddingType.INT8);

            AddEmbeddingsGenerationOperation addOp = new AddEmbeddingsGenerationOperation(config);
            AddEmbeddingsGenerationOperationResult addResult = store.maintenance().send(addOp);
            long taskId = addResult.getTaskId();

            assertThat(addResult.getTaskId()).isNotNull();
            assertThat(addResult.getRaftCommandIndex()).isNotNull();

            GetOngoingTaskInfoOperation getTaskOp =
                    new GetOngoingTaskInfoOperation(config.getName(), OngoingTaskType.EMBEDDINGS_GENERATION);

            OngoingTask task = store.maintenance().send(getTaskOp);

            assertThat(task).isNotNull();
            assertThat(task.getTaskId()).isEqualTo(taskId);
            assertThat(task.getTaskType()).isEqualTo(OngoingTaskType.EMBEDDINGS_GENERATION);
            assertThat(task.getTaskName()).isEqualTo(config.getName());
        }
    }

    private String putEmbeddingsConnectionString(IDocumentStore store) {
        String csName = "embeddings-" + System.currentTimeMillis();

        AiConnectionString aiConnectionString = new AiConnectionString();
        aiConnectionString.setName(csName);
        aiConnectionString.setModelType(AiModelType.TextEmbeddings);
        aiConnectionString.setEmbeddedSettings(new EmbeddedSettings());

        store.maintenance().send(new PutConnectionStringOperation(aiConnectionString));

        return csName;
    }
}
