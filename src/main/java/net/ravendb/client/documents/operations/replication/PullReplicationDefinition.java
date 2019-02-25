package net.ravendb.client.documents.operations.replication;

public class PullReplicationDefinition extends FeatureTaskDefinition {
    private String delayReplicationFor;
    private String mentorNode;

    public String getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(String delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }
}
