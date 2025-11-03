package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentHistoryConfiguration {
    private Integer historyExpirationInSec;
    public AiAgentHistoryConfiguration() {
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
