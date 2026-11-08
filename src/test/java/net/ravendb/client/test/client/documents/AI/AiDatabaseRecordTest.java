package net.ravendb.client.test.client.documents.AI;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.StartingPointChangeVector;
import net.ravendb.client.documents.operations.AI.*;
import net.ravendb.client.documents.operations.AI.agents.AddOrUpdateAiAgentOperation;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfiguration;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfigurationResult;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.1")
public class AiDatabaseRecordTest extends RemoteTestBase {

    @Test
    public void databaseRecordContainsEmbeddingsGenerations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putEmbeddingsConnectionString(store);

            EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
            config.setName("Products Embeddings");
            config.setCollection("Products");
            config.setConnectionStringName(csName);
            config.setIdentifier("products-embeddings");
            EmbeddingPathConfiguration path = new EmbeddingPathConfiguration();
            path.setPath("Description");
            path.setChunkingOptions(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));
            config.setEmbeddingsPathConfigurations(Collections.singletonList(path));
            config.setChunkingOptionsForQuerying(new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256, 0));
            config.setEmbeddingsCacheForQueryingExpiration(Duration.ofDays(3).plusHours(4));

            store.maintenance().send(new AddEmbeddingsGenerationOperation(config));

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record.getEmbeddingsGenerations()).hasSize(1);
            EmbeddingsGenerationConfiguration stored = record.getEmbeddingsGenerations().get(0);
            assertThat(stored.getIdentifier()).isEqualTo("products-embeddings");
            assertThat(stored.getEmbeddingsCacheExpiration()).isEqualTo(Duration.ofDays(90));
            assertThat(stored.getEmbeddingsCacheForQueryingExpiration()).isEqualTo(Duration.ofDays(3).plusHours(4));
        }
    }

    @Test
    public void embeddingsCacheExpirationsReadTimeSpanFormat() throws Exception {
        // records written by the C# client / Studio hold TimeSpan strings
        EmbeddingsGenerationConfiguration config = JsonExtensions.getDefaultMapper().readValue(
                "{\"EmbeddingsCacheExpiration\":\"90.00:00:00\",\"EmbeddingsCacheForQueryingExpiration\":\"14.00:00:00\"}",
                EmbeddingsGenerationConfiguration.class);
        assertThat(config.getEmbeddingsCacheExpiration()).isEqualTo(Duration.ofDays(90));
        assertThat(config.getEmbeddingsCacheForQueryingExpiration()).isEqualTo(Duration.ofDays(14));

        ObjectNode json = JsonExtensions.getDefaultMapper().valueToTree(new EmbeddingsGenerationConfiguration());
        assertThat(json.get("EmbeddingsCacheExpiration").asText()).isEqualTo("90.00:00:00");
        assertThat(json.get("Quantization").asText()).isEqualTo("Single");
        assertThat(json.has("TransformationName")).isFalse();
    }

    @Test
    public void databaseRecordContainsGenAis() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putChatConnectionString(store);

            GenAiConfiguration config = new GenAiConfiguration();
            config.setName("GenAiTask");
            config.setConnectionStringName(csName);
            config.setCollection("Users");
            config.setIdentifier("users-genai");
            config.setPrompt("Enrich user document: {{context}}");
            config.setSampleObject("{\"result\":\"sample\"}");
            config.setUpdateScript("function update(doc, result) { doc.genai = result; return doc; }");
            GenAiTransformation transformation = new GenAiTransformation();
            transformation.setScript("ai.genContext({ name: this.Name });");
            config.setGenAiTransformation(transformation);
            config.setDisabled(true);

            store.maintenance().send(new AddGenAiOperation(config, StartingPointChangeVector.LastDocument));

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record.getGenAis()).hasSize(1);
            assertThat(record.getGenAis().get(0).getIdentifier()).isEqualTo("users-genai");
            assertThat(record.getGenAis().get(0).getPrompt()).isEqualTo("Enrich user document: {{context}}");
        }
    }

    @Test
    public void aiAgentRoundTrip() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putChatConnectionString(store);

            AiAgentConfiguration agent = new AiAgentConfiguration("sample-agent", csName, "Be helpful");
            agent.setSampleObject("{\"answer\":\"text\"}");
            agent.setDisabled(true);

            // C# default: summarize by tokens
            assertThat(agent.getChatTrimming()).isNotNull();
            assertThat(agent.getChatTrimming().getTokens()).isNotNull();
            assertThat(agent.getQueries()).isEmpty();
            assertThat(agent.getActions()).isEmpty();
            assertThat(agent.getParameters()).isEmpty();

            AiAgentConfigurationResult result = store.ai().createAgent(agent);

            AiAgentConfiguration loaded = store.ai().getAgent(result.getIdentifier());
            assertThat(loaded.getSampleObject()).isEqualTo("{\"answer\":\"text\"}");
            assertThat(loaded.isDisabled()).isTrue();
            assertThat(loaded.getChatTrimming()).isNotNull();
            assertThat(loaded.getChatTrimming().getTokens()).isNotNull();

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
            assertThat(record.getAiAgents()).hasSize(1);
            assertThat(record.getAiAgents().get(0).getIdentifier()).isEqualTo(result.getIdentifier());
        }
    }

    @Test
    public void sampleObjectIsUsedOnlyWhenNoSchemaOrSampleIsSet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = putChatConnectionString(store);

            AiAgentConfiguration withSample = new AiAgentConfiguration("agent-with-sample", csName, "Be helpful");
            withSample.setSampleObject("{\"existing\":\"value\"}");
            AiAgentConfigurationResult r1 = store.ai().createAgent(withSample, new Answer("from-object"));
            assertThat(store.ai().getAgent(r1.getIdentifier()).getSampleObject()).isEqualTo("{\"existing\":\"value\"}");

            AiAgentConfiguration withoutSample = new AiAgentConfiguration("agent-without-sample", csName, "Be helpful");
            AiAgentConfigurationResult r2 = store.ai().createAgent(withoutSample, new Answer("from-object"));
            assertThat(store.ai().getAgent(r2.getIdentifier()).getSampleObject()).isEqualTo("{\"answer\":\"from-object\"}");

            AiAgentConfiguration empty = new AiAgentConfiguration("agent-empty", csName, "Be helpful");
            assertThatThrownBy(() -> store.maintenance().send(new AddOrUpdateAiAgentOperation(empty)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Please provide a non-empty value for either  or SampleObject is required.");
        }
    }

    @Test
    public void vertexConnectionStringRoundTrip() throws Exception {
        VertexSettings noCredentials = new VertexSettings("text-embedding-005", null, "us-central1");
        ObjectNode json = JsonExtensions.getDefaultMapper().valueToTree(noCredentials);
        assertThat(json.has("ProjectId")).isFalse();

        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString cs = new AiConnectionString();
            cs.setName("vertex");
            cs.setIdentifier("vertex");
            cs.setModelType(AiModelType.TextEmbeddings);
            cs.setVertexSettings(new VertexSettings("text-embedding-005", "{\"project_id\":\"my-project\",\"type\":\"service_account\"}", "us-central1", VertexAIVersion.V1));

            store.maintenance().send(new PutConnectionStringOperation(cs));

            GetConnectionStringsResult result = store.maintenance().send(new GetConnectionStringsOperation());
            VertexSettings loaded = result.getAiConnectionStrings().get("vertex").getVertexSettings();
            assertThat(loaded.getModel()).isEqualTo("text-embedding-005");
            assertThat(loaded.getLocation()).isEqualTo("us-central1");
            assertThat(loaded.getAiVersion()).isEqualTo(VertexAIVersion.V1);
            assertThat(loaded.getProjectId()).isEqualTo("my-project");
        }
    }

    @Test
    public void databaseRecordBuilderCanAddEmbeddingsGeneration() {
        EmbeddingsGenerationConfiguration config = new EmbeddingsGenerationConfiguration();
        config.setIdentifier("x");

        DatabaseRecord record = DatabaseRecordBuilder.create()
                .regular("db")
                .withEtls(b -> b.addEmbeddingsGeneration(config))
                .toDatabaseRecord();

        assertThat(record.getEmbeddingsGenerations()).containsExactly(config);
        assertThat(record.getGenAis()).isEmpty();
        assertThat(record.getAiAgents()).isEmpty();
    }

    public static class Answer {
        private String answer;

        public Answer() {
        }

        public Answer(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    private static String putEmbeddingsConnectionString(IDocumentStore store) {
        AiConnectionString cs = new AiConnectionString();
        cs.setName("embeddings");
        cs.setIdentifier("embeddings");
        cs.setModelType(AiModelType.TextEmbeddings);
        cs.setEmbeddedSettings(new EmbeddedSettings());
        store.maintenance().send(new PutConnectionStringOperation(cs));
        return cs.getName();
    }

    private static String putChatConnectionString(IDocumentStore store) {
        AiConnectionString cs = new AiConnectionString();
        cs.setName("chat");
        cs.setIdentifier("chat");
        cs.setModelType(AiModelType.Chat);
        cs.setOpenAiSettings(new OpenAiSettings("test", "https://api.openai.example", "gpt-test"));
        store.maintenance().send(new PutConnectionStringOperation(cs));
        return cs.getName();
    }
}
