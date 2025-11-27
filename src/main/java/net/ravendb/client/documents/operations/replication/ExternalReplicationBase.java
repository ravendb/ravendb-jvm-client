package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;

public abstract class ExternalReplicationBase extends ReplicationNode {
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

    @Override
    public boolean isEqualTo(ReplicationNode other) {
        if (other instanceof ExternalReplicationBase) {
            ExternalReplicationBase external = (ExternalReplicationBase) other;

            return connectionStringName.equalsIgnoreCase(external.getConnectionStringName())
                    && getDatabase().equalsIgnoreCase(external.getDatabase())
                    && taskId == external.getTaskId();
        }

        return false;
    }

    public long getTaskKey() {
        long hashCode = calculateStringHash(this.getDatabase());
        hashCode = (hashCode * 397) ^ calculateStringHash(connectionStringName);
        return (hashCode * 397) ^ (long) taskId;
    }

}
