package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Enumeration;

public class PullReplicationAsSink extends ExternalReplicationBase {

    private EnumSet<PullReplicationMode> mode = EnumSet.of(PullReplicationMode.HUB_TO_SINK);

    private String[] allowedHubToSinkPaths;
    private String[] allowedSinkToHubPaths;

    private String certificateWithPrivateKey;
    private String certificatePassword;

    private String accessName;

    private String hubName;

    public PullReplicationAsSink() {
    }

    public PullReplicationAsSink(String database, String connectionStringName, String hubName) {
        super(database, connectionStringName);
        this.hubName = hubName;
    }

    @JsonSerialize(using = JsonExtensions.SharpEnumSetSerializer.class)
    public EnumSet<PullReplicationMode> getMode() {
        return mode;
    }

    public void setMode(EnumSet<PullReplicationMode> mode) {
        this.mode = mode;
    }


    public String[] getAllowedHubToSinkPaths() {
        return allowedHubToSinkPaths;
    }

    public void setAllowedHubToSinkPaths(String[] allowedHubToSinkPaths) {
        this.allowedHubToSinkPaths = allowedHubToSinkPaths;
    }

    public String[] getAllowedSinkToHubPaths() {
        return allowedSinkToHubPaths;
    }

    public void setAllowedSinkToHubPaths(String[] allowedSinkToHubPaths) {
        this.allowedSinkToHubPaths = allowedSinkToHubPaths;
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


    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String getHubName() {
        return hubName;
    }

    public void setHubName(String hubName) {
        this.hubName = hubName;
    }

    boolean hasPrivateKey() {
        try {
            byte[] certBytes = Base64.getDecoder().decode(certificateWithPrivateKey);
            char[] password = certificatePassword != null ? certificatePassword.toCharArray() : new char[0];

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream stream = new ByteArrayInputStream(certBytes)) {
                keyStore.load(stream, password);
            }

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                if (keyStore.isKeyEntry(aliases.nextElement())) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to inspect certificate for a private key", e);
        }
    }
}
