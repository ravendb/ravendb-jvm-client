package net.ravendb.client.documents.AI;

import java.util.HashMap;
import java.util.Map;

/**
 * Options used when creating or continuing an AI conversation.
 * Allows passing required parameters for the agent tools and controlling
 * conversation expiration.
 */
public class AiConversationCreationOptions {
    private Map<String, AiConversationParameter> parameters;
    private Integer expirationInSec;
    private Integer maxModelIterationsPerCall;

    public AiConversationCreationOptions() {
    }

    /**
     * Initializes the options from a map of raw parameter values.
     * Values that are already {@link AiConversationParameter} are used as-is;
     * any other value is wrapped with {@code sendToModel = true}.
     */
    public AiConversationCreationOptions(Map<String, Object> parameters) {
        this(parameters, null);
    }

    public AiConversationCreationOptions(Map<String, Object> parameters, Integer expirationInSec) {
        if (parameters != null) {
            this.parameters = new HashMap<>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                Object value = entry.getValue();
                this.parameters.put(entry.getKey(), value instanceof AiConversationParameter
                        ? (AiConversationParameter) value
                        : new AiConversationParameter(value));
            }
        }
        this.expirationInSec = expirationInSec;
    }

    /**
     * Conversation-level parameters passed to the agent.
     * Each parameter defines a value and whether it should be sent to the model.
     *
     * @return the parameters map
     */
    public Map<String, AiConversationParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, AiConversationParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Adds a named parameter and value to the parameters map.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return this instance for fluent configuration
     */
    public AiConversationCreationOptions addParameter(String name, Object value) {
        return addParameter(name, value, null);
    }

    /**
     * Adds a named parameter and value to the parameters map.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @param options optional configuration controlling how the parameter is handled (e.g. sendToModel)
     * @return this instance for fluent configuration
     */
    public AiConversationCreationOptions addParameter(String name, Object value, AiConversationParameterOptions options) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(name, new AiConversationParameter(value, options == null || options.isSendToModel()));
        return this;
    }

    /**
     * Optional conversation expiration in seconds.
     * When specified, the server may expire the conversation document after this interval.
     *
     * @return the expiration in seconds
     */
    public Integer getExpirationInSec() {
        return expirationInSec;
    }

    public void setExpirationInSec(Integer expirationInSec) {
        this.expirationInSec = expirationInSec;
    }

    public Integer getMaxModelIterationsPerCall() {
        return maxModelIterationsPerCall;
    }

    public void setMaxModelIterationsPerCall(Integer maxModelIterationsPerCall) {
        this.maxModelIterationsPerCall = maxModelIterationsPerCall;
    }
}
