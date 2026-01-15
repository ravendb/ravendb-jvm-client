package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.Constants;

public class AiUsage {
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private long cachedTokens;
    private long reasoningTokens;

    public AiUsage() {
    }

    public AiUsage(long promptTokens, long completionTokens, long totalTokens, long cachedTokens, long reasoningTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.cachedTokens = cachedTokens;
        this.reasoningTokens = reasoningTokens;
    }

    public long getReasoningTokens() { return reasoningTokens; }

    public void setReasoningTokens(long reasoningTokens) { this.reasoningTokens = reasoningTokens; }

    public long getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public long getCachedTokens() {
        return cachedTokens;
    }

    public void setCachedTokens(long cachedTokens) {
        this.cachedTokens = cachedTokens;
    }

    void updateFrom(JsonNode json) {
        if (json.has(Constants.AI.PROMPT_TOKENS)) {
            this.promptTokens += json.get(Constants.AI.PROMPT_TOKENS).asLong();
        }
        if (json.has(Constants.AI.COMPLETION_TOKENS)) {
            this.completionTokens += json.get(Constants.AI.COMPLETION_TOKENS).asLong();
        }
        if (json.has(Constants.AI.TOTAL_TOKENS)) {
            this.totalTokens += json.get(Constants.AI.TOTAL_TOKENS).asLong();
        }

        JsonNode promptDetails = json.get(Constants.AI.PROMPT_TOKENS_DETAILS);
        if (promptDetails != null && promptDetails.has(Constants.AI.CACHED_TOKENS)) {
            this.cachedTokens += promptDetails.get(Constants.AI.CACHED_TOKENS).asLong();
        }

        JsonNode completionTokensDetails = json.get(Constants.AI.COMPLETION_TOKENS_DETAILS);
        if (completionTokensDetails != null && completionTokensDetails.has(Constants.AI.REASONING_TOKENS)) {
            this.reasoningTokens += completionTokensDetails.get(Constants.AI.REASONING_TOKENS).asLong();
        }
    }

    static AiUsage getUsageDifference(AiUsage current, AiUsage previous) {
        long previousTotalWithoutReasoning = (previous.getCompletionTokens() - previous.getReasoningTokens() + previous.getPromptTokens());

        AiUsage result = new AiUsage();
        result.setPromptTokens(Math.max(current.getPromptTokens() - previousTotalWithoutReasoning, 0));
        result.setTotalTokens(Math.max(current.getTotalTokens() - previousTotalWithoutReasoning, 0));
        result.setCachedTokens(current.getCachedTokens());        // keep as-is
        result.setCompletionTokens(current.getCompletionTokens()); // keep as-is
        result.setReasoningTokens(current.getReasoningTokens());   // keep as-is

        return result;
    }
}

