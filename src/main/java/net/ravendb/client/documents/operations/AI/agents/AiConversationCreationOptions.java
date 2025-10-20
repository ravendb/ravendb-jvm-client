package net.ravendb.client.documents.operations.AI.agents;

import java.util.Map;

public class AiConversationCreationOptions {
    private Map<String, Object> parameters; // Optional: key-value pairs
    private Integer expirationInSec;        // Optional: expiration time in seconds

    public AiConversationCreationOptions() {
        // Default constructor
    }

    public AiConversationCreationOptions(Map<String, Object> parameters, Integer expirationInSec) {
        this.parameters = parameters;
        this.expirationInSec = expirationInSec;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Integer getExpirationInSec() {
        return expirationInSec;
    }

    public void setExpirationInSec(Integer expirationInSec) {
        this.expirationInSec = expirationInSec;
    }
}

