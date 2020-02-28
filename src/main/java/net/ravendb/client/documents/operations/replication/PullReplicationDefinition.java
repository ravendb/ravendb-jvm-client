package net.ravendb.client.documents.operations.replication;

import java.time.Duration;

public class PullReplicationDefinition extends FeatureTaskDefinition {
    private Duration delayReplicationFor;
    private String mentorNode;

    public PullReplicationDefinition() {
    }

    public PullReplicationDefinition(String name) {
        setName(name);
    }

    public Duration getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(Duration delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }
}
