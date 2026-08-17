package net.ravendb.client.documents.operations.backups;

public class S3Settings extends AmazonSettings implements IS3Settings {

    private String bucketName;
    private String customServerUrl;
    private S3StorageClass storageClass;
    private boolean forcePathStyle;
    private boolean disableChecksumValidation;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getCustomServerUrl() {
        return customServerUrl;
    }

    public void setCustomServerUrl(String customServerUrl) {
        this.customServerUrl = customServerUrl;
    }

    public boolean isForcePathStyle() {
        return forcePathStyle;
    }

    public void setForcePathStyle(boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }

    @Override
    public boolean isDisableChecksumValidation() {
        return disableChecksumValidation;
    }

    @Override
    public void setDisableChecksumValidation(boolean disableChecksumValidation) {
        this.disableChecksumValidation = disableChecksumValidation;
    }

    @Override
    public S3StorageClass getStorageClass() {
        return storageClass;
    }

    @Override
    public void setStorageClass(S3StorageClass storageClass) {
        this.storageClass = storageClass;
    }

    public boolean hasSettings() {
        if (super.hasSettings()) {
            return true;
        }

        return bucketName != null && !bucketName.trim().isEmpty();
    }
}
