package net.ravendb.client.serverwide.tcp;

public class TcpConnectionHeaderResponse {

    private TcpConnectionStatus status;
    private String message;
    private int version;

    private LicensedFeatures licensedFeatures;

    public TcpConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(TcpConnectionStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LicensedFeatures getLicensedFeatures() {
        return licensedFeatures;
    }

    public void setLicensedFeatures(LicensedFeatures licensedFeatures) {
        this.licensedFeatures = licensedFeatures;
    }
}
