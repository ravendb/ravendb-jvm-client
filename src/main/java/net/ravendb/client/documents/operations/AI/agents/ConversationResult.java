package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.operations.AI.AiUsage;
import java.time.Duration;
import java.util.List;

public class ConversationResult<TAnswer> {
    private String conversationId;
    private String changeVector;
    private TAnswer response;
    private AiUsage totalUsage;
    private AiUsage usage;
    private Duration Elapsed;
    private List<AiAgentActionRequest> actionRequests;

    public ConversationResult() {
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public TAnswer getResponse() {
        return response;
    }

    public void setResponse(TAnswer response) {
        this.response = response;
    }

    public AiUsage getTotalUsage() {
        return totalUsage;
    }

    public AiUsage getUsage() {
        return usage;
    }
    public void setUsage(AiUsage usage) {
        this.usage = usage;
    }

    public Duration getElapsed() { return Elapsed; }

    public void setElapsed(Duration elapsed) { this.Elapsed = elapsed; }

    public void setTotalUsage(AiUsage totalUsage) {
        this.totalUsage = totalUsage;
    }

    public List<AiAgentActionRequest> getActionRequests() {
        return actionRequests;
    }

    public void setActionRequests(List<AiAgentActionRequest> actionRequests) {
        this.actionRequests = actionRequests;
    }
}
