package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentConfigurationResult {
    private String identifier;
    private int raftCommandIndex;

    public AiAgentConfigurationResult() {
        // Default constructor
    }

    public AiAgentConfigurationResult(String identifier, int raftCommandIndex) {
        this.identifier = identifier;
        this.raftCommandIndex = raftCommandIndex;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(int raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
