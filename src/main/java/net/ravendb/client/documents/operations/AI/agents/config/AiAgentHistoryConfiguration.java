package net.ravendb.client.documents.operations.AI.agents.config;

public class AiAgentHistoryConfiguration {
    private Integer historyExpirationInSec; // Optional: expiration time in seconds

    public AiAgentHistoryConfiguration() {
        // Default constructor
    }

    public AiAgentHistoryConfiguration(Integer historyExpirationInSec) {
        this.historyExpirationInSec = historyExpirationInSec;
    }

    public Integer getHistoryExpirationInSec() {
        return historyExpirationInSec;
    }

    public void setHistoryExpirationInSec(Integer historyExpirationInSec) {
        this.historyExpirationInSec = historyExpirationInSec;
    }
}
