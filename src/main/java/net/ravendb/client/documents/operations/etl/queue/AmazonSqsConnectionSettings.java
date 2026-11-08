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
