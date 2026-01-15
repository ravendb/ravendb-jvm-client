package net.ravendb.client.documents.operations.backups;

public class GoogleCloudSettings extends BackupSettings {
    private String bucketName;
    private String remoteFolderName;
    private String googleCredentialsJson;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRemoteFolderName() {
        return remoteFolderName;
    }

    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }

    public String getGoogleCredentialsJson() {
        return googleCredentialsJson;
    }

    public void setGoogleCredentialsJson(String googleCredentialsJson) {
        this.googleCredentialsJson = googleCredentialsJson;
    }

    @Override
    public boolean hasSettings() {
        if (super.hasSettings()) {
            return true;
        }

        return bucketName != null && !bucketName.trim().isEmpty();
    }
}
