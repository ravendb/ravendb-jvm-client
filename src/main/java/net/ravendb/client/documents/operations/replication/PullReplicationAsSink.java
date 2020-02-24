package net.ravendb.client.documents.operations.replication;

public class PullReplicationAsSink extends ExternalReplicationBase {

    private String certificateWithPrivateKey;
    private String certificatePassword;
    private String hubDefinitionName;

    public PullReplicationAsSink() {
    }

    public PullReplicationAsSink(String database, String connectionStringName, String hubDefinitionName) {
        super(database, connectionStringName);
        this.hubDefinitionName = hubDefinitionName;
    }

    public String getCertificateWithPrivateKey() {
        return certificateWithPrivateKey;
    }

    public void setCertificateWithPrivateKey(String certificateWithPrivateKey) {
        this.certificateWithPrivateKey = certificateWithPrivateKey;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getHubDefinitionName() {
        return hubDefinitionName;
    }

    public void setHubDefinitionName(String hubDefinitionName) {
        this.hubDefinitionName = hubDefinitionName;
    }
}
