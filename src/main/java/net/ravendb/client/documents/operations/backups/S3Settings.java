package net.ravendb.client.documents.operations.backups;

public class S3Settings extends AmazonSettings {

    private String bucketName;
    private String remoteFolderName;

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
}
