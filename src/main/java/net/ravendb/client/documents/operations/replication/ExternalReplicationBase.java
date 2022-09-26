package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;

public class ExternalReplicationBase extends ReplicationNode {
    private long taskId;
    private String name;
    private String connectionStringName;
    private String mentorNode;

    private boolean pinToMentorNode;

    protected ExternalReplicationBase() {
    }

    protected ExternalReplicationBase(String database, String connectionStringName) {
        this.setDatabase(database);
        this.setConnectionStringName(connectionStringName);
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
    }
}
