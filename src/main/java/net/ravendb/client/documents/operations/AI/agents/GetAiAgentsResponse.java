package net.ravendb.client.documents.operations.AI.agents;

import java.util.List;

public class GetAiAgentsResponse {
    private List<AiAgentConfiguration> aiAgents;

    public GetAiAgentsResponse() {
    }

    public GetAiAgentsResponse(List<AiAgentConfiguration> aiAgents) {
        this.aiAgents = aiAgents;
    }

    public List<AiAgentConfiguration> getAiAgents() {
        return aiAgents;
    }

    public void setAiAgents(List<AiAgentConfiguration> aiAgents) {
        this.aiAgents = aiAgents;
    }
}
