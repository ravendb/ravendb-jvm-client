package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.operations.AI.AiUsage;

import java.util.List;

public class ConversationResult<TAnswer> {
    private String conversationId;
    private String changeVector;
    private TAnswer response;
    private AiUsage totalUsage;
    private List<AiAgentActionRequest> actionRequests;

    public ConversationResult() {
    }

    public ConversationResult(String conversationId, String changeVector, TAnswer response,
                              AiUsage totalUsage, List<AiAgentActionRequest> actionRequests) {
        this.conversationId = conversationId;
        this.changeVector = changeVector;
        this.response = response;
        this.totalUsage = totalUsage;
        this.actionRequests = actionRequests;
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
