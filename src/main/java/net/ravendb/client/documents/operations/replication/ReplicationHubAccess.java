package net.ravendb.client.documents.operations.replication;

/**
 * Represents the configuration for replication hub access.
 * This class allows you to define the access permissions for replication between a hub and sink.
 */
public class ReplicationHubAccess {
    /**
     * The name of the replication hub access configuration.
     */
    private String name;
    /**
     * The Base64-encoded certificate used to authenticate the access.
     */
    private String certificateBase64;
    /**
     * An array of allowed paths for data replication from the hub to the sink.
     */
    private String[] allowedHubToSinkPaths;
    /**
     * An array of allowed paths for data replication from the sink to the hub.
     */
    private String[] allowedSinkToHubPaths;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCertificateBase64() {
        return certificateBase64;
    }

    public void setCertificateBase64(String certificateBase64) {
        this.certificateBase64 = certificateBase64;
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
}
