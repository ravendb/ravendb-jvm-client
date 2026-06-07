package net.ravendb.client.documents.AI;

/**
 * Optional configuration for a conversation parameter.
 * Allows controlling how the parameter is handled when sent to the model.
 */
public class AiConversationParameterOptions {

    private boolean sendToModel = true;

    public AiConversationParameterOptions() {
    }

    public AiConversationParameterOptions(boolean sendToModel) {
        this.sendToModel = sendToModel;
    }

    /**
     * Determines whether the parameter should be sent to the model.
     * <p>
     * The parameter will be included in the model input only if this flag is set to {@code true}
     * and the parameter is also allowed by the agent configuration.
     * <p>
     * When set to {@code false}, the parameter remains available for internal use
     * (e.g., queries, actions, or sub-agents) but is not exposed to the model.
     * <p>
     * Default is {@code true}.
     *
     * @return whether the parameter should be sent to the model
     */
    public boolean isSendToModel() {
        return sendToModel;
    }

    public void setSendToModel(boolean sendToModel) {
        this.sendToModel = sendToModel;
    }
}
