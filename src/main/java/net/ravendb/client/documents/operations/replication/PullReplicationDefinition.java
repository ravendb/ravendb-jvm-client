package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;

public class PullReplicationDefinition {

    private Map<String, String> certificates; // <thumbprint, base64 cert>

    private Duration delayReplicationFor;
    private boolean disabled;

    private String mentorNode;

    private EnumSet<PullReplicationMode> mode = EnumSet.of(PullReplicationMode.HUB_TO_SINK);

    private String name;
    private long taskId;

    private boolean withFiltering;

    public PullReplicationDefinition() {
    }

    public PullReplicationDefinition(String name) {
        this.name = name;
    }

    /**
     * @deprecated You cannot use Certificates on the PullReplicationDefinition any more, please use the dedicated commands:
     *  RegisterReplicationHubAccessOperation and UnregisterReplicationHubAccessOperation
     */
    public Map<String, String> getCertificates() {
        return certificates;
    }

    /**
     * @deprecated You cannot use Certificates on the PullReplicationDefinition any more, please use the dedicated commands:
     *  RegisterReplicationHubAccessOperation and UnregisterReplicationHubAccessOperation
     */
    public void setCertificates(Map<String, String> certificates) {
        this.certificates = certificates;
    }

    public Duration getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(Duration delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    @JsonSerialize(using = JsonExtensions.SharpEnumSetSerializer.class)
    public EnumSet<PullReplicationMode> getMode() {
        return mode;
    }

    public void setMode(EnumSet<PullReplicationMode> mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isWithFiltering() {
        return withFiltering;
    }

    public void setWithFiltering(boolean withFiltering) {
        this.withFiltering = withFiltering;
    }
}
