package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentArtificialActionResponse {

    public String toolId;
    public String content;

    public void validate() {
        if (toolId == null || toolId.trim().isEmpty()) {
            throw new IllegalArgumentException("toolId");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content");
        }
    }
}

