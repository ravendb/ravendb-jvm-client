package net.ravendb.client.test.client.documents.AI;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.agents.AddOrUpdateAiAgentOperation;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfigurationResult;
import net.ravendb.client.documents.operations.AI.agents.AiAgentParameter;
import net.ravendb.client.documents.operations.AI.agents.AiAgentToolQuery;
import net.ravendb.client.documents.operations.AI.agents.config.AiAgentConfiguration;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class AiAgentTests extends RemoteTestBase {

    @Test
    public void canCreateAiAgent() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
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

                AddOrUpdateAiAgentOperation createOp = new AddOrUpdateAiAgentOperation(agentConfiguration, null);
                AiAgentConfigurationResult result = store.maintenance().send(createOp);

                assertThat(result).isNotNull();
                assertThat(result.getIdentifier()).isNotNull();
                assertThat(result.getRaftCommandIndex()).isGreaterThan(0);

                AiAgentConfiguration agentResponse = store.getAiOperations().getAgent(result.getIdentifier()).join();

                assertThat(agentResponse).isNotNull();
                assertThat(agentResponse.getName()).isEqualTo(agentConfiguration.getName());
                assertThat(agentResponse.getConnectionStringName()).isEqualTo(agentConfiguration.getConnectionStringName());
                assertThat(agentResponse.getSystemPrompt()).isEqualTo(agentConfiguration.getSystemPrompt());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void canUpdateAiAgent() {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
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

                CompletableFuture<AiAgentConfiguration> future = store.getAiOperations().getAgent(createRes.getIdentifier()); // or getIdentifier()
                AiAgentConfiguration agent = future.join();

                assertNotNull(agent);
                assertEquals("updated prompt", agent.getSystemPrompt());
                assertNotNull(agent.getParameters());
                assertEquals(1, agent.getParameters().size());
                assertEquals("p", agent.getParameters().get(0).getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
