package net.ravendb.client.serverwide.operations.certificates;

public class CertificateDefinition extends CertificateMetadata {

    private String certificate;
    private String password;

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
