package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentChatTrimmingConfiguration {
    private AiAgentSummarizationByTokens tokens;
    private AiAgentHistoryConfiguration history;

    public AiAgentChatTrimmingConfiguration() {
    }

    public AiAgentChatTrimmingConfiguration(AiAgentSummarizationByTokens tokens, AiAgentHistoryConfiguration history) {
        this.tokens = tokens;
        this.history = history;
    }

    public AiAgentSummarizationByTokens getTokens() {
        return tokens;
    }

    public void setTokens(AiAgentSummarizationByTokens tokens) {
        this.tokens = tokens;
    }

    public AiAgentHistoryConfiguration getHistory() {
        return history;
    }

    public void setHistory(AiAgentHistoryConfiguration history) {
        this.history = history;
    }
}
