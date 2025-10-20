package net.ravendb.client.documents.operations.AI.agents.config;

import net.ravendb.client.documents.operations.AI.agents.AiAgentParameter;
import net.ravendb.client.documents.operations.AI.agents.AiAgentToolAction;
import net.ravendb.client.documents.operations.AI.agents.AiAgentToolQuery;

import java.util.List;

public class AiAgentConfiguration {
    private String identifier; // Optional
    private String name;
    private String connectionStringName;
    private String systemPrompt;
    private String sampleObject; // JSON string sample for output
    private String outputSchema; // Optional: JSON schema for output
    private List<AiAgentToolQuery> queries; // Optional
    private List<AiAgentToolAction> actions; // Optional
    private List<AiAgentParameter> parameters; // Optional
    private AiAgentChatTrimmingConfiguration chatTrimming; // Optional
    private Integer maxModelIterationsPerCall; // Optional

    public AiAgentConfiguration() {
        // Default constructor
    }

    public AiAgentConfiguration(String identifier, String name, String connectionStringName, String systemPrompt,
                                String sampleObject, String outputSchema, List<AiAgentToolQuery> queries,
                                List<AiAgentToolAction> actions, List<AiAgentParameter> parameters,
                                AiAgentChatTrimmingConfiguration chatTrimming, Integer maxModelIterationsPerCall) {
        this.identifier = identifier;
        this.name = name;
        this.connectionStringName = connectionStringName;
        this.systemPrompt = systemPrompt;
        this.sampleObject = sampleObject;
        this.outputSchema = outputSchema;
        this.queries = queries;
        this.actions = actions;
        this.parameters = parameters;
        this.chatTrimming = chatTrimming;
        this.maxModelIterationsPerCall = maxModelIterationsPerCall;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getSampleObject() {
        return sampleObject;
    }

    public void setSampleObject(String sampleObject) {
        this.sampleObject = sampleObject;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public List<AiAgentToolQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<AiAgentToolQuery> queries) {
        this.queries = queries;
    }

    public List<AiAgentToolAction> getActions() {
        return actions;
    }

    public void setActions(List<AiAgentToolAction> actions) {
        this.actions = actions;
    }

    public List<AiAgentParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<AiAgentParameter> parameters) {
        this.parameters = parameters;
    }

    public AiAgentChatTrimmingConfiguration getChatTrimming() {
        return chatTrimming;
    }

    public void setChatTrimming(AiAgentChatTrimmingConfiguration chatTrimming) {
        this.chatTrimming = chatTrimming;
    }

    public Integer getMaxModelIterationsPerCall() {
        return maxModelIterationsPerCall;
    }

    public void setMaxModelIterationsPerCall(Integer maxModelIterationsPerCall) {
        this.maxModelIterationsPerCall = maxModelIterationsPerCall;
    }
}
