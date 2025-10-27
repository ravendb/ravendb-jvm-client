package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.agents.*;
import net.ravendb.client.documents.operations.AI.agents.config.AiAgentConfiguration;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;

import java.util.concurrent.CompletableFuture;

public class AiOperations {
    private final IDocumentStore store;
    private final String databaseName;
    private final MaintenanceOperationExecutor executor;

    /**
     * Initializes a new instance of AiOperations for a given document store and optional database name.
     */
    public AiOperations(IDocumentStore store, String databaseName) {
        this.store = store;
        this.databaseName = databaseName;
        this.executor = this.store.maintenance().forDatabase(this.databaseName);
    }

    public AiOperations(IDocumentStore store) {
        this.store = store;
        this.databaseName = store.getDatabase();
        this.executor = this.store.maintenance().forDatabase(this.databaseName);
    }

    /**
     * Returns an AiOperations instance for a different database.
     */
    public AiOperations forDatabase(String databaseName) {
        if (this.databaseName.equalsIgnoreCase(databaseName)) {
            return this;
        }
        return new AiOperations(this.store, databaseName);
    }

    /**
     * Creates or updates an AI agent configuration (with the given schema) on the database.
     */
    public <TSchema> AiAgentConfigurationResult createAgent(
            AiAgentConfiguration configuration,
            TSchema sampleObject) {
        AddOrUpdateAiAgentOperation operation = new AddOrUpdateAiAgentOperation(configuration, sampleObject);
        return executor.send(operation);
    }

    /**
     * Retrieves the AI agent configuration for a specific agent.
     */
    public AiAgentConfiguration getAgent(String agentId) {
        GetAiAgentsOperation operation = new GetAiAgentsOperation(agentId);
        GetAiAgentsResponse response = executor.send(operation);
        if (response.getAiAgents() != null && !response.getAiAgents().isEmpty()) {
            return response.getAiAgents().get(0);
        }
        return null;
    }

    /**
     * Retrieves all AI agents and their configurations.
     */
    public GetAiAgentsResponse getAgents() {
        GetAiAgentsOperation operation = new GetAiAgentsOperation();
        return executor.send(operation);
    }

    /**
     * Deletes an AI agent configuration.
     */
    public AiAgentConfigurationResult deleteAgent(String identifier) {
        DeleteAiAgentOperation operation = new DeleteAiAgentOperation(identifier);
        return executor.send(operation);
    }

    /**
     * Opens an AI conversation for an agent.
     */
    public AiConversation conversation(String agentId, String conversationId) {
        return new AiConversation(store, databaseName, agentId, conversationId, null, null);
    }

    /**
     * Opens an AI conversation for an agent.
     */
    public AiConversation conversation(String agentId, String conversationId,
                                       AiConversationCreationOptions creationOptions,
                                       String changeVector) {
        return new AiConversation(store, databaseName, agentId, conversationId, creationOptions, changeVector);
    }
}
