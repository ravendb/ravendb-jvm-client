package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentConfigurationResult {
    private String identifier;
    private long raftCommandIndex;

    public AiAgentConfigurationResult() {
    }

    public AiAgentConfigurationResult(String identifier, long raftCommandIndex) {
        this.identifier = identifier;
        this.raftCommandIndex = raftCommandIndex;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
