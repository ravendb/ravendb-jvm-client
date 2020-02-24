package net.ravendb.client.documents.operations.backups;

public abstract class AmazonSettings extends BackupSettings {
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsSessionToken;
    private String awsRegionName;
    private String remoteFolderName;

    public String getRemoteFolderName() {
        return remoteFolderName;
    }

    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }

    public String getAwsSessionToken() {
        return awsSessionToken;
    }

    public void setAwsSessionToken(String awsSessionToken) {
        this.awsSessionToken = awsSessionToken;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsRegionName() {
        return awsRegionName;
    }

    public void setAwsRegionName(String awsRegionName) {
        this.awsRegionName = awsRegionName;
    }
}
