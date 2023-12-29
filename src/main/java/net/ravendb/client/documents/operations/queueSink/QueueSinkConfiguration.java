package net.ravendb.client.documents.operations.queueSink;

import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;

import java.util.List;

public class QueueSinkConfiguration {

    private QueueBrokerType brokerType;
    private long taskId;
    private boolean disabled;
    private String name;
    private String mentorNode;
    private boolean pinToMentorNode;
    private String connectionStringName;
    private List<QueueSinkScript> scripts;

    public QueueBrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(QueueBrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public List<QueueSinkScript> getScripts() {
        return scripts;
    }

    public void setScripts(List<QueueSinkScript> scripts) {
        this.scripts = scripts;
    }
}
