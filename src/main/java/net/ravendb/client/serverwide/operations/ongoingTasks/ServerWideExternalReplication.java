package net.ravendb.client.serverwide.operations.ongoingTasks;

import net.ravendb.client.documents.operations.replication.IExternalReplication;

import java.time.Duration;

public class ServerWideExternalReplication implements IExternalReplication, IServerWideTask {
    private boolean disabled;
    private long taskId;
    private String name;
    private String mentorNode;
    private Duration delayReplicationFor;
    private String[] topologyDiscoveryUrls;
    private String[] excludedDatabases;

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public long getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getMentorNode() {
        return mentorNode;
    }

    @Override
    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    @Override
    public Duration getDelayReplicationFor() {
        return delayReplicationFor;
    }

    @Override
    public void setDelayReplicationFor(Duration delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    public String[] getTopologyDiscoveryUrls() {
        return topologyDiscoveryUrls;
    }

    public void setTopologyDiscoveryUrls(String[] topologyDiscoveryUrls) {
        this.topologyDiscoveryUrls = topologyDiscoveryUrls;
    }

    @Override
    public String[] getExcludedDatabases() {
        return excludedDatabases;
    }

    @Override
    public void setExcludedDatabases(String[] excludedDatabases) {
        this.excludedDatabases = excludedDatabases;
    }

}
