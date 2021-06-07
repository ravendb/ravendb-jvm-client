package net.ravendb.client.documents.operations.replication;

import java.time.Duration;

public interface IExternalReplication {

    boolean isDisabled();

    void setDisabled(boolean disabled);

    long getTaskId();

    void setTaskId(long taskId);

    String getName();

    void setName(String name);

    String getMentorNode();

    void setMentorNode(String mentorNode);

    Duration getDelayReplicationFor();

    void setDelayReplicationFor(Duration delayReplicationFor);
}
