package net.ravendb.client.documents.operations.etl;

import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;

import java.util.ArrayList;
import java.util.List;

public abstract class EtlConfiguration<T extends ConnectionString> {
    private long taskId;
    private String name;
    private String mentorNode;
    private String connectionStringName;
    private List<Transformation> transforms = new ArrayList<>();
    private boolean disabled;
    private boolean allowEtlOnNonEncryptedChannel;

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

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public List<Transformation> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<Transformation> transforms) {
        this.transforms = transforms;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isAllowEtlOnNonEncryptedChannel() {
        return allowEtlOnNonEncryptedChannel;
    }

    public void setAllowEtlOnNonEncryptedChannel(boolean allowEtlOnNonEncryptedChannel) {
        this.allowEtlOnNonEncryptedChannel = allowEtlOnNonEncryptedChannel;
    }
}
