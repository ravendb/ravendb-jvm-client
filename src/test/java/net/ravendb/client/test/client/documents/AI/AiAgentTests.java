package net.ravendb.client.test.client.documents.AI;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.AI.AiAnswer;
import net.ravendb.client.documents.AI.AiConversation;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.AiConversationResult;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.AiConnectionString;
import net.ravendb.client.documents.operations.AI.AiModelType;
import net.ravendb.client.documents.operations.AI.OpenAiSettings;
import net.ravendb.client.documents.operations.AI.agents.*;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfiguration;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsOperation;
import net.ravendb.client.documents.operations.connectionStrings.GetConnectionStringsResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringResult;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@EnableOnServer(thresholdVersion = "7.1")
public class AiAgentTests extends RemoteTestBase {

    @Disabled
    @Test
    public void canStreamResults(){
        String apiKey = System.getenv("RAVENDB_JAVA_TESTS_OPENAI_API_KEY");
        assertNotNull(apiKey, "OpenAI API key is not set in environment variable RAVENDB_JAVA_TESTS_OPENAI_API_KEY");
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                OpenAiSettings ai = new OpenAiSettings(apiKey,
                        "https://api.openai.com/",
                        "gpt-4o-mini",
                        null,
                        null,
                        null,
                        0.0);
                AiConnectionString openAiCs = new AiConnectionString();
                openAiCs.setModelType(AiModelType.Chat);
                openAiCs.setName("openai");
                openAiCs.setOpenAiSettings(ai);

                IMaintenanceOperation<PutConnectionStringResult> putOpenAi = new PutConnectionStringOperation(openAiCs);
                store.maintenance().send(putOpenAi);

                GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
                assertThat(connectionStrings.getAiConnectionStrings()).hasSize(1);
                assertThat(connectionStrings.getAiConnectionStrings().get("openai")).isNotNull();

                AiAgentConfiguration agent = new AiAgentConfiguration("my assistant", openAiCs.getName(), "Be helpful");
                String identifier = store.ai().createAgent(agent,AnswerSchema.INSTANCE).getIdentifier();
                AiConversation chat = store.ai().conversation(identifier,"chats/", new AiConversationCreationOptions());
                chat.setUserPrompt("Give me 15 real cities names, one per line");
                StringBuilder sb = new StringBuilder();
                AiAnswer<AnswerSchema> result = chat.<AnswerSchema>stream(AnswerSchema::getAnswer,
                        s-> {
                            sb.append(s);
                            return  CompletableFuture.completedFuture(null);
                        }).get();
                Object answerObj = result.getAnswer();
                if (answerObj instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) answerObj;
                    String text = (String) map.get("answer");
                    assertEquals(sb.toString(), text);
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Disabled
    @Test
    public void AiAgentClientApiBasicTest(){
        String apiKey = System.getenv("RAVENDB_JAVA_TESTS_OPENAI_API_KEY");
        assertNotNull(apiKey, "OpenAI API key is not set in environment variable RAVENDB_JAVA_TESTS_OPENAI_API_KEY");
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                OpenAiSettings ai = new OpenAiSettings(apiKey,
                        "https://api.openai.com/",
                        "gpt-4o-mini",
                        null,
                        null,
                        null,
                        0.0);

                AiConnectionString openAiCs = new AiConnectionString();
                openAiCs.setModelType(AiModelType.Chat);
                openAiCs.setName("openai");
                openAiCs.setOpenAiSettings(ai);

                IMaintenanceOperation<PutConnectionStringResult> putOpenAi = new PutConnectionStringOperation(openAiCs);
                store.maintenance().send(putOpenAi);

                GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
                assertThat(connectionStrings.getAiConnectionStrings()).hasSize(1);
                assertThat(connectionStrings.getAiConnectionStrings().get("openai")).isNotNull();

                AiAgentConfiguration agent = new AiAgentConfiguration("shopping assistant", openAiCs.getName(), "You are an AI agent of an online shop, helping customers answer queries about that topic only. When talking about orders or products, include the ids as well.");
                agent.getParameters().add(new AiAgentParameter("company"));
                List<AiAgentToolQuery> queries = new ArrayList<>();

                AiAgentToolQuery query1 = new AiAgentToolQuery("ProductSearch", "semantic search the store product catalog", "from Products where vector.search(embedding.text(Name), $query)");
                query1.setParametersSampleObject("{\"query\": [\"term or phrase to search in the catalog\"]}");

                AiAgentToolQuery query2 = new AiAgentToolQuery("RecentOrder", "Get the recent orders of the current user", "from Orders where Company = $company order by OrderedAt desc limit 10");
                query2.setParametersSampleObject("{}");

                queries.add(query1);
                queries.add(query2);
                agent.setQueries(queries);
                String identifier = store.ai().createAgent(agent,AnswerSchema.INSTANCE).getIdentifier();
                AiConversation chat = store.ai().conversation(identifier,"chats/", new AiConversationCreationOptions().addParameter("company", "companies/90-A"));

                chat.setUserPrompt("what goes well with my cheese?");
                AiAnswer<AnswerSchema> result = chat.<AnswerSchema>run().get();
                assertEquals(AiConversationResult.Done, result.getStatus());
                assertNotNull(result.getAnswer());
                assertNotNull(chat.getId());

                chat.setUserPrompt("what goes well with my cheese?");
                result = chat.<AnswerSchema>run().get();
                assertEquals(AiConversationResult.Done, result.getStatus());
                assertNotNull(result.getAnswer());

                chat.setUserPrompt("what cheese goes well with italian food?");
                result = chat.<AnswerSchema>run().get();
                assertEquals(AiConversationResult.Done, result.getStatus());
                assertNotNull(result.getAnswer());
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @DisabledOnPullRequest
    @Test
    public void canCreateAiAgent() {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = "r1-" + System.currentTimeMillis();

            RavenConnectionString ravenConnectionString = new RavenConnectionString();
            ravenConnectionString.setDatabase(store.getDatabase());
            ravenConnectionString.setTopologyDiscoveryUrls(new String[]{"http://localhost:8080"});
            ravenConnectionString.setName(csName);

            store.maintenance().send(new PutConnectionStringOperation(ravenConnectionString));

            AiAgentConfiguration agentConfiguration = new AiAgentConfiguration();
            agentConfiguration.setName("TestAgent-" + System.currentTimeMillis());
            agentConfiguration.setConnectionStringName(csName);
            agentConfiguration.setSystemPrompt("You are a helpful assistant for querying document data.");
            agentConfiguration.setSampleObject("{\"result\":\"sample result data\",\"queryTime\":\"time taken to process query\"}");

            AiAgentParameter parameter = new AiAgentParameter();
            parameter.setName("query");
            parameter.setDescription("The query to execute against the database");
            agentConfiguration.setParameters(Collections.singletonList(parameter));

            AiAgentToolQuery query = new AiAgentToolQuery();
            query.setName("execute-query");
            query.setDescription("Executes the provided query against the database");
            query.setQuery("from @collection as c where c.name == $queryName select c");
            query.setParametersSampleObject("{\"queryName\":\"Example query parameter\"}");
            agentConfiguration.setQueries(Collections.singletonList(query));

            AddOrUpdateAiAgentOperation createOp = new AddOrUpdateAiAgentOperation(agentConfiguration);
            AiAgentConfigurationResult result = store.maintenance().send(createOp);

            assertThat(result).isNotNull();
            assertThat(result.getIdentifier()).isNotNull();
            assertThat(result.getRaftCommandIndex()).isGreaterThan(0);

            AiAgentConfiguration agentResponse = store.ai().getAgent(result.getIdentifier());

            assertThat(agentResponse).isNotNull();
            assertThat(agentResponse.getName()).isEqualTo(agentConfiguration.getName());
            assertThat(agentResponse.getConnectionStringName()).isEqualTo(agentConfiguration.getConnectionStringName());
            assertThat(agentResponse.getSystemPrompt()).isEqualTo(agentConfiguration.getSystemPrompt());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @DisabledOnPullRequest
    @Test
    public void canUpdateAiAgent() {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = "r1-" + System.currentTimeMillis();

            RavenConnectionString ravenConnectionString = new RavenConnectionString();
            ravenConnectionString.setDatabase(store.getDatabase());
            ravenConnectionString.setTopologyDiscoveryUrls(new String[]{"http://localhost:8080"});
            ravenConnectionString.setName(csName);

            store.maintenance().send(new PutConnectionStringOperation(ravenConnectionString));

            String name = "Agent-" + System.currentTimeMillis();

            AiAgentConfiguration initialConfig = new AiAgentConfiguration();
            initialConfig.setName(name);
            initialConfig.setConnectionStringName(csName);
            initialConfig.setSystemPrompt("initial prompt");
            initialConfig.setSampleObject("{\"foo\":\"bar\"}");
            initialConfig.setMaxModelIterationsPerCall(2);
            initialConfig.setQueries(new ArrayList<>());

            AiAgentConfigurationResult createRes = store.maintenance().send(new AddOrUpdateAiAgentOperation(initialConfig));

            AiAgentConfiguration updatedConfig = new AiAgentConfiguration();
            updatedConfig.setName(name);
            updatedConfig.setConnectionStringName(csName);
            updatedConfig.setSystemPrompt("updated prompt");
            updatedConfig.setSampleObject("{\"foo\":\"bar\"}");
            updatedConfig.setMaxModelIterationsPerCall(2);
            updatedConfig.setQueries(new ArrayList<>());

            AiAgentParameter param = new AiAgentParameter();
            param.setName("p");
            param.setDescription("param");
            updatedConfig.setParameters(Collections.singletonList(param));

            store.maintenance().send(new AddOrUpdateAiAgentOperation(updatedConfig));

            AiAgentConfiguration agent = store.ai().getAgent(createRes.getIdentifier());
            assertNotNull(agent);
            assertEquals("updated prompt", agent.getSystemPrompt());
            assertNotNull(agent.getParameters());
            assertEquals(1, agent.getParameters().size());
            assertEquals("p", agent.getParameters().get(0).getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DisabledOnPullRequest
    @Test
    public void canListAndDeleteAiAgent() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String csName = "r1-" + System.currentTimeMillis();
            RavenConnectionString ravenConnectionString = new RavenConnectionString();
            ravenConnectionString.setDatabase(store.getDatabase());
            ravenConnectionString.setTopologyDiscoveryUrls(new String[]{"http://localhost:8080"});
            ravenConnectionString.setName(csName);

            store.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString));
            String name = "agent-" + System.currentTimeMillis();
            AiAgentConfiguration config = new AiAgentConfiguration();
            config.setName(name);
            config.setConnectionStringName(csName);
            config.setSystemPrompt("prompt");
            config.setSampleObject("{\"a\":1}");
            config.setQueries(Collections.emptyList());
            AddOrUpdateAiAgentOperation addOp = new AddOrUpdateAiAgentOperation(config);
            AiAgentConfigurationResult res = store.maintenance().send(addOp);

            GetAiAgentsResponse list = store.ai().getAgents();
            assertThat(list).isNotNull();
            assertThat(list.getAiAgents()).isNotNull();

            AiAgentConfiguration found = list.getAiAgents()
                    .stream()
                    .filter(a -> name.equals(a.getName()))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNotNull();

            AiAgentConfigurationResult delRes = store.ai().deleteAgent(res.getIdentifier());
            assertThat(delRes).isNotNull();

            GetAiAgentsResponse afterDelete = store.ai().getAgents();
            assertThat(afterDelete.getAiAgents().size()).isEqualTo(0);
        }
    }

    @Test
    public void cannotCreateAgentWithoutSchemaOrSampleObject() {
        try (IDocumentStore store = getDocumentStore()) {
            AiAgentConfiguration badConfig = new AiAgentConfiguration();
            badConfig.setName("BadAgent-" + System.currentTimeMillis());
            badConfig.setConnectionStringName("cs");
            badConfig.setSystemPrompt("prompt");
            badConfig.setQueries(Collections.emptyList());
            try {
                store.maintenance()
                        .send(new AddOrUpdateAiAgentOperation(badConfig));
                Assertions.assertTrue(false, "Expected exception not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("Please provide a non-empty value for either outputSchema or sampleObject.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class AnswerSchema {
        public static final AnswerSchema INSTANCE = new AnswerSchema();
        public String answer = "Answer to the user question";
        public boolean relevant = true;
        public List<String> relevantOrdersId = new ArrayList<>(
                Arrays.asList("The order ids relevant to the query or response")
        );
        public List<String> matchingProductsId = new ArrayList<>(
                Arrays.asList("All the product ids referenced either by the user or the system")
        );
        private AnswerSchema() {
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
