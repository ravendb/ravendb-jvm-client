package net.ravendb.client.serverwide.operations;

public class BuildNumber {

    private String productVersion;
    private int buildVersion;
    private String commitHash;
    private String fullVersion;

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public int getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(int buildVersion) {
        this.buildVersion = buildVersion;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }
}
