package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;

public class ExternalReplication extends ReplicationNode {
    private long taskId;
    private String name;
    private String connectionStringName;
    private String mentorName;

    public ExternalReplication() {
    }

    public ExternalReplication(String database, String connectionStringName) {
        setDatabase(database);
        setConnectionStringName(connectionStringName);
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

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }
}
