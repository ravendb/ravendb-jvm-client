package net.ravendb.client.documents.operations.etl.queue;

public final class AmazonSqsConnectionSettings {

    private AmazonSqsCredentials basic;
    private boolean passwordless;
    private boolean useEmulator;

    public AmazonSqsCredentials getBasic() {
        return basic;
    }

    public void setBasic(AmazonSqsCredentials basic) {
        this.basic = basic;
    }

    public boolean isPasswordless() {
        return passwordless;
    }

    public void setPasswordless(boolean passwordless) {
        this.passwordless = passwordless;
    }

    boolean isUseEmulator() {
        return useEmulator;
    }

    void setUseEmulator(boolean useEmulator) {
        this.useEmulator = useEmulator;
    }
}

final class AmazonSqsCredentials {

    private String accessKey;
    private String secretKey;
    private String regionName;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}
