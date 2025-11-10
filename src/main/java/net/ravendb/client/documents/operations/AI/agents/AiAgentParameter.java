package net.ravendb.client.documents.operations.AI.agents;

import java.util.HashMap;
import java.util.Map;

public class AiAgentParameter {
    private String name;
    private String description;
    private Boolean sendToModel;

    public AiAgentParameter() {
    }

    public AiAgentParameter(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name cannot be null or empty");
        this.name = name;
    }

    public AiAgentParameter(String name, String description) {
        this(name);
        if (description == null || description.isEmpty())
            throw new IllegalArgumentException("Description cannot be null or empty");
        this.description = description;
    }

    /**
     * Initializes a new agent parameter and controls whether its value should be sent to the LLM.
     * Use this constructor when you need to explicitly hide sensitive values
     * (e.g., userId, tenant, or company) from the model.
     *
     * @param name the parameter name; cannot be null or empty.
     * @param description a human-readable description; may be null or empty when using this constructor.
     * @param sendToModel when {@code false}, the parameter is hidden from the model
     *                    (it will not be included in prompts/echo messages).
     *                    When {@code true}, the parameter is exposed to the model.
     *                    If this constructor is not called, the default is {@code null}
     *                    (treated as exposed).
     */
    public AiAgentParameter(String name, String description, Boolean sendToModel) {
        this(name, description);
        this.sendToModel = sendToModel;
    }

    public Boolean getSendToModel() {
        return sendToModel;
    }

    public void setSendToModel(Boolean sendToModel) {
        this.sendToModel = sendToModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("Name", this.name);
        json.put("Description", this.description);
        json.put("SendToModel", this.sendToModel);
        return json;
    }
}

