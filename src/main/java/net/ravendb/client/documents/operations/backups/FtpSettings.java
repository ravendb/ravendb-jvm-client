package net.ravendb.client.documents.operations.backups;

public class FtpSettings extends BackupSettings {
    private String url;
    private String userName;
    private String password;
    private String certificateAsBase64;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertificateAsBase64() {
        return certificateAsBase64;
    }

    public void setCertificateAsBase64(String certificateAsBase64) {
        this.certificateAsBase64 = certificateAsBase64;
    }

}
