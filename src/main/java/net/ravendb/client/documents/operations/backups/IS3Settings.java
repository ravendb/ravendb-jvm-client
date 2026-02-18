package net.ravendb.client.documents.operations.backups;

public interface IS3Settings {

    String getBucketName();
    void setBucketName(String bucketName);

    String getCustomServerUrl();
    void setCustomServerUrl(String customServerUrl);

    boolean isForcePathStyle();
    void setForcePathStyle(boolean forcePathStyle);

    S3StorageClass getStorageClass();
    void setStorageClass(S3StorageClass storageClass);

    String getAwsAccessKey();
    void setAwsAccessKey(String awsAccessKey);

    String getAwsSecretKey();
    void setAwsSecretKey(String awsSecretKey);

    String getAwsSessionToken();
    void setAwsSessionToken(String awsSessionToken);

    String getAwsRegionName();
    void setAwsRegionName(String awsRegionName);

    String getRemoteFolderName();
    void setRemoteFolderName(String remoteFolderName);
}
