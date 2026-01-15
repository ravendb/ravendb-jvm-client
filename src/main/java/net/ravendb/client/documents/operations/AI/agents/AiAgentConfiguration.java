package net.ravendb.client.documents.operations.AI.agents;

import java.util.ArrayList;
import java.util.List;

public class AiAgentConfiguration {
    private String identifier;
    private String name;
    private String connectionStringName;
    private String systemPrompt;
    private String sampleObject;
    private String outputSchema;
    private List<AiAgentToolQuery> queries;
    private List<AiAgentToolAction> actions;
    private List<AiAgentParameter> parameters;
    private AiAgentChatTrimmingConfiguration chatTrimming;
    private Integer maxModelIterationsPerCall;
    /**
     * Indicates whether the AI agent is disabled.
     */
    private boolean disabled;

    public AiAgentConfiguration() {
    }

    public AiAgentConfiguration(String name, String connectionStringName, String systemPrompt) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (connectionStringName == null || connectionStringName.isEmpty()) {
            throw new IllegalArgumentException("connectionStringName cannot be null or empty");
        }
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            throw new IllegalArgumentException("systemPrompt cannot be null or empty");
        }

        this.name = name;
        this.connectionStringName = connectionStringName;
        this.systemPrompt = systemPrompt;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
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
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
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
