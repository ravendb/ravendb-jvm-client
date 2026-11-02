package net.ravendb.client.test.client.documents.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.AI.AiAnswer;
import net.ravendb.client.documents.AI.AiConversation;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.AiConversationResult;
import net.ravendb.client.documents.AI.AiOutputOptions;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.AiConnectionString;
import net.ravendb.client.documents.operations.AI.AiModelType;
import net.ravendb.client.documents.operations.AI.OpenAiSettings;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfiguration;
import net.ravendb.client.documents.operations.AI.agents.AiAgentToolAction;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.test.client.documents.AI.AiOutputOptionsTest.AlternativeSchema;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.2")
public class RavenDB_24824Test extends RemoteTestBase {

    @Test
    public void invalidOutputOptionsCombinationsShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation("agents/test", "chats/123", new AiConversationCreationOptions());
            chat.setUserPrompt("hello");

            assertThatThrownBy(() -> chat.runWithSchema((AiOutputOptions) null))
                    .isInstanceOf(IllegalArgumentException.class);

            AiOutputOptions schemaWithNoSchema = new AiOutputOptions("{}");
            schemaWithNoSchema.setNoSchema(true);
            assertThatThrownBy(() -> chat.runWithSchema(schemaWithNoSchema))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("noSchema");

            AiOutputOptions sampleWithNoSchema = new AiOutputOptions(new AlternativeSchema("x", 1));
            sampleWithNoSchema.setNoSchema(true);
            assertThatThrownBy(() -> chat.runWithSchema(sampleWithNoSchema))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("noSchema");
            assertThatThrownBy(() -> chat.streamWithSchema("summary", chunk -> CompletableFuture.completedFuture(null), sampleWithNoSchema))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("noSchema");

            // a String passed to the generic sample-object overload is a sample object, not a schema - reject it like C#
            assertThatThrownBy(() -> chat.streamWithSchema("summary", chunk -> CompletableFuture.completedFuture(null), "{\"type\":\"object\"}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("raw string");

            // a structured answer still needs a property to stream
            assertThatThrownBy(() -> chat.streamWithSchema("", chunk -> CompletableFuture.completedFuture(null), new AiOutputOptions("{}")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // the tests below need a live OpenAI model and run only when the API key is set (non-blank),
    // using the same environment variable as the C# tests (RavenTestHelper.EnvironmentVariables.AiIntegrationOpenAiApiKeyEnvName)
    private static final String OPENAI_API_KEY = "RAVEN_AI_INTEGRATION_OPENAI_API_KEY";
    private static final String NOT_BLANK = "(?s).*\\S.*";

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void outputSchemaFieldTrackedPerTurnInDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/1", new AiConversationCreationOptions());

            // turn 1: default schema
            chat.setUserPrompt("Say hello.");
            chat.run().get();

            // turn 2: sample object override
            chat.setUserPrompt("Rate how helpful you are on a scale of 1 to 10 and summarize what you do.");
            chat.runWithSchema(new AiOutputOptions(new AlternativeSchema("a short summary", 5))).get();

            // turn 3: explicit schema override
            chat.setUserPrompt("Rate your usefulness 1-10 and summarize in one sentence.");
            AiAnswer<Object> turn3 = chat.runWithSchema(new AiOutputOptions(EXPLICIT_SCHEMA)).get();
            assertAlternativeSchemaAnswer(turn3.getAnswer());

            // turn 4: raw text
            chat.setUserPrompt("Say goodbye.");
            AiAnswer<String> turn4 = chat.runWithoutSchema().get();
            assertThat(turn4.getAnswer()).isNotEmpty();

            // turn 5: back to the default schema, no leftovers from the override turns
            chat.setUserPrompt("What did we talk about?");
            chat.run().get();

            try (IDocumentSession session = store.openSession()) {
                ObjectNode conversation = session.load(ObjectNode.class, "chats/1");

                List<String> outputSchemas = new ArrayList<>();
                for (JsonNode message : conversation.get("Messages")) {
                    if ("assistant".equals(message.path("role").asText())) {
                        outputSchemas.add(message.hasNonNull("output_schema") ? message.get("output_schema").asText() : null);
                    }
                }

                // only a NoSchema turn is tracked in the document
                assertThat(outputSchemas).containsExactly(null, null, null, "none", null);
            }
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void runWithoutSchemaReturnsRawString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/", new AiConversationCreationOptions());

            chat.setUserPrompt("Say hello in one sentence.");
            AiAnswer<String> answer = chat.runWithoutSchema().get();

            assertThat(answer.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat(answer.getAnswer()).isNotEmpty();
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void streamWithoutSchemaStreamsRawText() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/", new AiConversationCreationOptions());

            StringBuilder streamed = new StringBuilder();
            chat.setUserPrompt("Tell me a short fact about databases.");
            AiAnswer<String> answer = chat.streamWithoutSchema(chunk -> {
                streamed.append(chunk);
                return CompletableFuture.completedFuture(null);
            }).get();

            assertThat(answer.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat(answer.getAnswer()).isNotEmpty().isEqualTo(streamed.toString());
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void convenienceOverloadsWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/", new AiConversationCreationOptions());

            // sample object overload - like run(), the answer arrives as a map
            chat.setUserPrompt("Rate how helpful you are on a scale of 1 to 10 and summarize what you do.");
            AiAnswer<Object> r1 = chat.<Object>runWithSchema(new AlternativeSchema("a short summary", 5)).get();
            assertThat(r1.getStatus()).isEqualTo(AiConversationResult.Done);
            assertAlternativeSchemaAnswer(r1.getAnswer());
            assertThat(r1.getUsage().getTotalTokens()).isPositive();
            assertThat(r1.getElapsed()).isGreaterThan(Duration.ZERO);

            // schema string overload
            chat.setUserPrompt("Rate your usefulness again 1-10 and summarize in one sentence.");
            AiAnswer<Object> r2 = chat.runWithSchema(EXPLICIT_SCHEMA).get();
            assertThat(r2.getStatus()).isEqualTo(AiConversationResult.Done);
            assertAlternativeSchemaAnswer(r2.getAnswer());
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void canHandleNoArgsTool() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiAgentConfiguration agent = new AiAgentConfiguration("time assistant", putOpenAiConnectionString(store),
                    "You are a helpful assistant. When asked for the current time, you MUST call the get_current_time tool first, then answer.");
            AiAgentToolAction getCurrentTime = new AiAgentToolAction("get_current_time", "Returns the current UTC time. Takes no parameters.");
            getCurrentTime.setParametersSampleObject("{}");
            agent.setActions(Collections.singletonList(getCurrentTime));

            String agentId = store.ai().createAgent(agent, new DefaultSchema("the answer")).getIdentifier();
            AiConversation chat = store.ai().conversation(agentId, "chats/", new AiConversationCreationOptions());

            AtomicBoolean toolCalled = new AtomicBoolean(false);
            chat.handle("get_current_time", () -> {
                toolCalled.set(true);
                return Instant.now().toString();
            });

            chat.setUserPrompt("What is the current time? Use the get_current_time tool.");
            AiAnswer<Object> result = chat.run().get();

            assertThat(result.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat(toolCalled.get()).as("the no-args tool handler was called").isTrue();
            assertThat(asMap(result.getAnswer()).get("answer")).isNotNull();
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void multiTurnConversationResumesCorrectlyAfterNoSchemaTurn() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/1", new AiConversationCreationOptions());

            chat.setUserPrompt("Say hello.");
            AiAnswer<String> turn1 = chat.runWithoutSchema().get();
            assertThat(turn1.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat(turn1.getAnswer()).isNotNull();

            // after a NoSchema turn, the default schema must still produce structured output
            chat.setUserPrompt("Repeat the greeting you just said.");
            AiAnswer<Object> turn2 = chat.run().get();
            assertThat(turn2.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat((String) asMap(turn2.getAnswer()).get("answer")).isNotEmpty();
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @EnabledIfEnvironmentVariable(named = OPENAI_API_KEY, matches = NOT_BLANK)
    @Test
    public void canStreamWithSchemaOverride() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation chat = store.ai().conversation(createSimpleAgent(store), "chats/", new AiConversationCreationOptions());

            StringBuilder streamed = new StringBuilder();
            chat.setUserPrompt("Rate how helpful you are on a scale of 1 to 10 and summarize what you do.");
            AiAnswer<Object> answer = chat.<Object>streamWithSchema("summary", chunk -> {
                streamed.append(chunk);
                return CompletableFuture.completedFuture(null);
            }, new AlternativeSchema("a short summary", 5)).get();

            assertThat(answer.getStatus()).isEqualTo(AiConversationResult.Done);
            assertThat(streamed.toString()).isNotEmpty();
            assertAlternativeSchemaAnswer(answer.getAnswer());
            assertThat(asMap(answer.getAnswer()).get("summary")).isEqualTo(streamed.toString());
        }
    }

    private static String putOpenAiConnectionString(IDocumentStore store) {
        String apiKey = System.getenv(OPENAI_API_KEY);

        AiConnectionString connectionString = new AiConnectionString();
        connectionString.setName("openai");
        connectionString.setModelType(AiModelType.Chat);
        connectionString.setOpenAiSettings(new OpenAiSettings(apiKey, "https://api.openai.com/", "gpt-4o-mini", null, null, null, 0.0));
        store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

        return connectionString.getName();
    }

    private static String createSimpleAgent(IDocumentStore store) {
        AiAgentConfiguration agent = new AiAgentConfiguration("test-assistant", putOpenAiConnectionString(store),
                "You are a helpful assistant. Answer questions concisely.");
        return store.ai().createAgent(agent, new DefaultSchema("the answer")).getIdentifier();
    }

    private static void assertAlternativeSchemaAnswer(Object answer) {
        Map<String, Object> map = asMap(answer);
        assertThat(map.get("summary")).isNotNull();
        assertThat(((Number) map.get("score")).intValue()).isPositive();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object answer) {
        assertThat(answer).isInstanceOf(Map.class);
        return (Map<String, Object>) answer;
    }

    private static final String EXPLICIT_SCHEMA = "{" +
            "\"name\": \"alt_output\"," +
            "\"strict\": true," +
            "\"schema\": {" +
            "  \"type\": \"object\"," +
            "  \"properties\": {" +
            "    \"summary\": { \"type\": \"string\" }," +
            "    \"score\": { \"type\": \"integer\" }" +
            "  }," +
            "  \"required\": [\"summary\", \"score\"]," +
            "  \"additionalProperties\": false" +
            "}}";

    public static class DefaultSchema {
        private String answer;

        public DefaultSchema() {
        }

        public DefaultSchema(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
