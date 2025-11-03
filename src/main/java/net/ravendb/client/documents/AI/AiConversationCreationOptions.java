package net.ravendb.client.documents.AI;

import java.util.HashMap;
import java.util.Map;

public class AiConversationCreationOptions {
    private Map<String, Object> parameters;
    private Integer expirationInSec;

    public AiConversationCreationOptions() {
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

    public AiConversationCreationOptions addParameter(String name, Object value) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(name, value);
        return this;
    }

    public Integer getExpirationInSec() {
        return expirationInSec;
    }

    public void setExpirationInSec(Integer expirationInSec) {
        this.expirationInSec = expirationInSec;
    }
}

