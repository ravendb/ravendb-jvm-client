package net.ravendb.client.documents.operations.AI;

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
}

