package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.Constants;

public class AiUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private int cachedTokens;
    private int reasoningTokens;

    public AiUsage() {
    }

    public AiUsage(int promptTokens, int completionTokens, int totalTokens, int cachedTokens, int reasoningTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.cachedTokens = cachedTokens;
        this.reasoningTokens = reasoningTokens;
    }

    public int getReasoningTokens() { return reasoningTokens; }

    public void setReasoningTokens(int reasoningTokens) { this.reasoningTokens = reasoningTokens; }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getCachedTokens() {
        return cachedTokens;
    }

    public void setCachedTokens(int cachedTokens) {
        this.cachedTokens = cachedTokens;
    }

    void updateFrom(JsonNode json) {
        if (json.has(Constants.AI.PROMPT_TOKENS)) {
            this.promptTokens += json.get(Constants.AI.PROMPT_TOKENS).asInt();
        }
        if (json.has(Constants.AI.COMPLETION_TOKENS)) {
            this.completionTokens += json.get(Constants.AI.COMPLETION_TOKENS).asInt();
        }
        if (json.has(Constants.AI.TOTAL_TOKENS)) {
            this.totalTokens += json.get(Constants.AI.TOTAL_TOKENS).asInt();
        }

        JsonNode promptDetails = json.get(Constants.AI.PROMPT_TOKENS_DETAILS);
        if (promptDetails != null && promptDetails.has(Constants.AI.CACHED_TOKENS)) {
            this.cachedTokens += promptDetails.get(Constants.AI.CACHED_TOKENS).asInt();
        }

        JsonNode completionTokensDetails = json.get(Constants.AI.COMPLETION_TOKENS_DETAILS);
        if (completionTokensDetails != null && completionTokensDetails.has(Constants.AI.REASONING_TOKENS)) {
            this.reasoningTokens += completionTokensDetails.get(Constants.AI.REASONING_TOKENS).asInt();
        }
    }

    static AiUsage getUsageDifference(AiUsage current, AiUsage previous) {
        int previousTotalWithoutReasoning = (previous.getCompletionTokens() - previous.getReasoningTokens() + previous.getPromptTokens());

        AiUsage result = new AiUsage();
        result.setPromptTokens(Math.max(current.getPromptTokens() - previousTotalWithoutReasoning, 0));
        result.setTotalTokens(Math.max(current.getTotalTokens() - previousTotalWithoutReasoning, 0));
        result.setCachedTokens(current.getCachedTokens());        // keep as-is
        result.setCompletionTokens(current.getCompletionTokens()); // keep as-is
        result.setReasoningTokens(current.getReasoningTokens());   // keep as-is

        return result;
    }
}

