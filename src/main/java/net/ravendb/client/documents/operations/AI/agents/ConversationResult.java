package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.operations.AI.AiUsage;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public static <TAnswer> ConversationResult<TAnswer> convert(
            JsonNode response,
            Class<TAnswer> answerClass
    ) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode totalUsageNode = response.get("TotalUsage");
        JsonNode resultNode = response.get("Response");
        String conversationId = response.has("ConversationId") ? response.get("ConversationId").asText() : null;
        String changeVector = response.has("ChangeVector") ? response.get("ChangeVector").asText() : null;
        JsonNode usageNode = response.get("Usage");
        Duration elapsed = response.has("Elapsed") ? Duration.parse(response.get("Elapsed").asText()) : null;

        List<AiAgentActionRequest> requests = StreamSupport.stream(response.path("ActionRequests").spliterator(), false)
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, AiAgentActionRequest.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        TAnswer answer = null;
        if (resultNode != null && !resultNode.isNull()) {
            answer = mapper.treeToValue(resultNode, answerClass);
        }

        ConversationResult<TAnswer> result = new ConversationResult<>();
        result.setConversationId(conversationId);
        result.setChangeVector(changeVector);
        result.setActionRequests(requests);
        result.setTotalUsage(totalUsageNode != null ? mapper.treeToValue(totalUsageNode, AiUsage.class) : null);
        result.setResponse(answer);
        result.setUsage(usageNode != null ? mapper.treeToValue(usageNode, AiUsage.class) : null);
        result.setElapsed(elapsed);

        return result;
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
