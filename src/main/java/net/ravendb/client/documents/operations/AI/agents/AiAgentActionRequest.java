package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentActionRequest {
    private String name;
    private String toolId;
    private String arguments;
    private AiAgentActionRequestType type = AiAgentActionRequestType.USER_ACTION;
    private String subConversationId;

    public AiAgentActionRequest() {
    }

    public AiAgentActionRequest(String name, String toolId, String arguments) {
        this.name = name;
        this.toolId = toolId;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public AiAgentActionRequestType getType() {
        return type;
    }

    public void setType(AiAgentActionRequestType type) {
        this.type = type;
    }

    public String getSubConversationId() {
        return subConversationId;
    }

    public void setSubConversationId(String subConversationId) {
        this.subConversationId = subConversationId;
    }
}
