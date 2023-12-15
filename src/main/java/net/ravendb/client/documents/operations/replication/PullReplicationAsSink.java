package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;
import org.apache.commons.lang3.ObjectUtils;

import java.util.EnumSet;

public class PullReplicationAsSink extends ExternalReplicationBase {

    private EnumSet<PullReplicationMode> mode = EnumSet.of(PullReplicationMode.HUB_TO_SINK);

    private String[] allowedHubToSinkPaths;
    private String[] allowedSinkToHubPaths;

    private String certificateWithPrivateKey;
    private String certificatePassword;

    private String accessName;

    private String hubName;

    public PullReplicationAsSink() {
    }

    public PullReplicationAsSink(String database, String connectionStringName, String hubName) {
        super(database, connectionStringName);
        this.hubName = hubName;
    }

    @JsonSerialize(using = JsonExtensions.SharpEnumSetSerializer.class)
    public EnumSet<PullReplicationMode> getMode() {
        return mode;
    }

    public void setMode(EnumSet<PullReplicationMode> mode) {
        this.mode = mode;
    }


    public String[] getAllowedHubToSinkPaths() {
        return allowedHubToSinkPaths;
    }

    public void setAllowedHubToSinkPaths(String[] allowedHubToSinkPaths) {
        this.allowedHubToSinkPaths = allowedHubToSinkPaths;
    }

    public String[] getAllowedSinkToHubPaths() {
        return allowedSinkToHubPaths;
    }

    public void setAllowedSinkToHubPaths(String[] allowedSinkToHubPaths) {
        this.allowedSinkToHubPaths = allowedSinkToHubPaths;
    }

    public String getCertificateWithPrivateKey() {
        return certificateWithPrivateKey;
    }

    public void setCertificateWithPrivateKey(String certificateWithPrivateKey) {
        this.certificateWithPrivateKey = certificateWithPrivateKey;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }


    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String getHubName() {
        return hubName;
    }

    public void setHubName(String hubName) {
        this.hubName = hubName;
    }
}
