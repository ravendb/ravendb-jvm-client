package net.ravendb.client.documents.operations.replication;

public class ReplicationHubAccess {

    private String name;
    private String certificateBase64;

    private String[] allowedHubToSinkPaths;
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
