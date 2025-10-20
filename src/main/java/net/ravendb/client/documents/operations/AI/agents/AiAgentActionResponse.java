package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentActionResponse {
    private String toolId;
    private String content; // JSON/string content provided back to the agent

    public AiAgentActionResponse() {
        // Default constructor
    }

    public AiAgentActionResponse(String toolId, String content) {
        this.toolId = toolId;
        this.content = content;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
