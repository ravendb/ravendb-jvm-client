package net.ravendb.client.documents.AI;

/**
 * Represents a single conversation parameter, including its value
 * and whether it should be sent to the model.
 */
public class AiConversationParameter {

    private Object value;
    private boolean sendToModel = true;

    public AiConversationParameter() {
    }

    public AiConversationParameter(Object value) {
        this.value = value;
    }

    public AiConversationParameter(Object value, boolean sendToModel) {
        this.value = value;
        this.sendToModel = sendToModel;
    }

    /**
     * @return The parameter value.
     */
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Controls whether this parameter is sent to the model at the conversation level.
     * The parameter will be included only if it is also allowed in the agent configuration
     * (sendToModel = true or unset there) and this flag is true.
     * Default is true.
     *
     * @return whether the parameter is sent to the model
     */
    public boolean isSendToModel() {
        return sendToModel;
    }

    public void setSendToModel(boolean sendToModel) {
        this.sendToModel = sendToModel;
    }
}
