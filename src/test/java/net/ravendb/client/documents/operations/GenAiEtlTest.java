package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.StartingPointChangeVector;
import net.ravendb.client.documents.operations.AI.*;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperationResult;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GenAiEtlTest extends RemoteTestBase {

    @DisabledOnPullRequest
    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canAddGenAiEtlTask() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putAiConnectionString(store);

            GenAiConfiguration config = createBaseGenAiConfiguration(csName);
            config.setPrompt("Enrich user document: {{context}}");
            config.setSampleObject("{\"result\":\"sample\"}");
            config.setUpdateScript("function update(doc, result) { doc.genai = result; return doc; }");

            AddGenAiOperation op =
                    new AddGenAiOperation(config, StartingPointChangeVector.LastDocument);

            AddGenAiOperationResult result = store.maintenance().send(op);

            assertThat(result).isNotNull();
            assertThat(result.getIdentifier()).isEqualTo(config.getIdentifier());
            assertThat(result.getTaskId()).isGreaterThan(0);
            assertThat(result.getRaftCommandIndex()).isGreaterThan(0);
        }
    }

    @DisabledOnPullRequest
    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canUpdateGenAiEtlTaskAndChangeStartingPoint() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putAiConnectionString(store);

            GenAiConfiguration config = createBaseGenAiConfiguration(csName);
            config.setPrompt("Enrich user document: {{context}}");
            config.setSampleObject("{\"result\":\"sample\"}");
            config.setUpdateScript("function update(doc, result) { doc.genai = result; return doc; }");

            AddGenAiOperation addOp = new AddGenAiOperation(config, StartingPointChangeVector.LastDocument);
            AddGenAiOperationResult addResult = store.maintenance().send(addOp);

            GenAiConfiguration updatedConfig = createBaseGenAiConfiguration(csName);
            updatedConfig.setName(config.getName());
            updatedConfig.setCollection(config.getCollection());
            updatedConfig.setIdentifier(config.getIdentifier());
            updatedConfig.setPrompt("Updated prompt: {{context}}");
            updatedConfig.setSampleObject(config.getSampleObject());
            updatedConfig.setUpdateScript(config.getUpdateScript());

            GenAiTransformation updatedTransformation = new GenAiTransformation();
            updatedTransformation.setScript("ai.genContext({ name: this.Name, updated: true });");
            updatedConfig.setGenAiTransformation(updatedTransformation);

            UpdateGenAiOperation updateOp = new UpdateGenAiOperation(addResult.getTaskId(), updatedConfig, StartingPointChangeVector.BeginningOfTime, true);
            UpdateEtlOperationResult updateResult = store.maintenance().send(updateOp);

            assertThat(updateResult).isNotNull();
            assertThat(updateResult.getTaskId()).isEqualTo(addResult.getTaskId() + 1);
            assertThat(updateResult.getRaftCommandIndex()).isGreaterThan(0);
        }
    }

    private GenAiConfiguration createBaseGenAiConfiguration(String csName) {
        GenAiConfiguration config = new GenAiConfiguration();
        config.setName("GenAiTask-" + System.currentTimeMillis());
        config.setConnectionStringName(csName);
        config.setCollection("Users");
        config.setIdentifier("users-genai");

        GenAiTransformation transformation = new GenAiTransformation();
        transformation.setScript("ai.genContext({ name: this.Name });");

        config.setGenAiTransformation(transformation);

        return config;
    }

    private String putAiConnectionString(IDocumentStore store) {
        String csName = "genai-" + System.currentTimeMillis();

        AiConnectionString aiConnectionString = new AiConnectionString();
        aiConnectionString.setName(csName);
        aiConnectionString.setIdentifier("openai-test");
        aiConnectionString.setModelType(AiModelType.Chat);
        aiConnectionString.setOpenAiSettings(
                new OpenAiSettings("test", "https://api.openai.example", "gpt-test")
        );

        store.maintenance().send(new PutConnectionStringOperation(aiConnectionString));

        return csName;
    }
}
