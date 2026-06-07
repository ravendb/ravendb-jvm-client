package net.ravendb.client.documents.operations.AI.agents;

import java.util.HashMap;
import java.util.Map;

public class AiAgentParameter {
    private String name;
    private String description;
    private Boolean sendToModel;
    private AiAgentParameterPolicy policy = AiAgentParameterPolicy.DEFAULT;
    private AiAgentParameterValueType type = AiAgentParameterValueType.DEFAULT;

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

    /**
     * Initializes a new agent parameter and controls whether its value should be sent to the LLM,
     * with policy flags for sub-agent behavior.
     *
     * @param name the parameter name; cannot be null or empty.
     * @param description a human-readable description.
     * @param sendToModel when {@code false}, the parameter is hidden from the model.
     * @param policy policy flags for this parameter. Use {@link AiAgentParameterPolicy#FORBID_MODEL_GENERATION}
     *               to prevent the parent agent from generating a value for this parameter when the agent
     *               is used as a sub-agent. The value may only be inherited from the parent agent,
     *               if a parameter with the same name exists.
     */
    public AiAgentParameter(String name, String description, Boolean sendToModel, AiAgentParameterPolicy policy) {
        this(name, description, sendToModel);
        this.policy = policy;
    }

    /**
     * Initializes a new agent parameter with a name, description, and policy flags.
     *
     * @param name the parameter name; cannot be null or empty.
     * @param description a human-readable description.
     * @param policy policy flags for this parameter. When {@link AiAgentParameterPolicy#FORBID_MODEL_GENERATION}
     *               is set and this agent is used as a sub-agent, the parent agent cannot generate a value
     *               for this parameter; it may only be inherited from the parent agent's parameters.
     */
    public AiAgentParameter(String name, String description, AiAgentParameterPolicy policy) {
        this(name, description);
        this.policy = policy;
    }

    /**
     * Initializes a new agent parameter with full control over model visibility, policy, and value type.
     *
     * @param name the parameter name; cannot be null or empty.
     * @param description a human-readable description.
     * @param sendToModel when {@code false}, the parameter is hidden from the model.
     * @param policy policy flags for this parameter.
     * @param type the expected {@link AiAgentParameterValueType} for this parameter. When set to a concrete
     *             value, the agent validates the provided value against it;
     *             {@link AiAgentParameterValueType#DEFAULT} disables type validation (backward compatibility).
     */
    public AiAgentParameter(String name, String description, Boolean sendToModel, AiAgentParameterPolicy policy, AiAgentParameterValueType type) {
        this(name, description, sendToModel, policy);
        this.type = type;
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

    /**
     * @return Policy flags defining how this parameter behaves when a sub-agent defines a parameter with the same name.
     */
    public AiAgentParameterPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(AiAgentParameterPolicy policy) {
        this.policy = policy;
    }

    /**
     * @return The expected JSON value type for this parameter. {@link AiAgentParameterValueType#DEFAULT} disables type validation.
     */
    public AiAgentParameterValueType getType() {
        return type;
    }

    public void setType(AiAgentParameterValueType type) {
        this.type = type;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("Name", this.name);
        json.put("Description", this.description);
        json.put("SendToModel", this.sendToModel);
        json.put("Policy", this.policy);
        json.put("Type", this.type);
        return json;
    }
}

