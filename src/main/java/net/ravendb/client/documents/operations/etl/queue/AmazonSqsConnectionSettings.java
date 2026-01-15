package net.ravendb.client.documents.operations.etl.queue;

public final class AmazonSqsConnectionSettings {

    static final String EMULATOR_URL_ENVIRONMENT_VARIABLE = "RAVEN_AMAZON_SQS_EMULATOR_URL";

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

    public boolean isValidConnection() {
        if (!isOnlyOneConnectionProvided()) {
            return false;
        }

        if (basic != null && !basic.isValid()) {
            return false;
        }

        return true;
    }

    private boolean isOnlyOneConnectionProvided() {
        int count = 0;

        if (basic != null)
            count++;

        if (passwordless)
            count++;

        if (useEmulator)
            count++;

        return count == 1;
    }

    public String getQueueUrl() {
        return useEmulator
                ? System.getenv(EMULATOR_URL_ENVIRONMENT_VARIABLE)
                : "https://queue.amazonaws.com/";
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

    public boolean isValid() {
        return isNotBlank(accessKey)
                && isNotBlank(secretKey)
                && isNotBlank(regionName);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
