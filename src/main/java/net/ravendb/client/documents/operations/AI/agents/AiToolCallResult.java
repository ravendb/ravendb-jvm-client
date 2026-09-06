package net.ravendb.client.documents.operations.AI.agents;

public class AiToolCallResult {

    private String id;
    private String name;
    private String arguments;
    private String result;
    private String subConversationId;

    /**
     * @return the tool call ID from the model
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the tool name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the arguments the model passed, as a JSON string
     */
    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * @return the tool's response content, or null if still pending (action required)
     */
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return if this tool call was a sub-agent invocation, the ID of the spawned sub-conversation.
     *         Can be queried separately via getConversationMessages.
     */
    public String getSubConversationId() {
        return subConversationId;
    }

    public void setSubConversationId(String subConversationId) {
        this.subConversationId = subConversationId;
    }
}
