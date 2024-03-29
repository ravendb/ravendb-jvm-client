package net.ravendb.client.serverwide.operations.certificates;

import java.util.*;

public class CertificateMetadata {

    private String name;
    private SecurityClearance securityClearance;
    private String thumbprint;
    private Date notAfter;
    private Date notBefore;
    private Map<String, DatabaseAccess> permissions = new TreeMap<>(String::compareToIgnoreCase);
    private List<String> collectionSecondaryKeys = new ArrayList<>();
    private String collectionPrimaryKey = "";
    private String publicKeyPinningHash;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SecurityClearance getSecurityClearance() {
        return securityClearance;
    }

    public void setSecurityClearance(SecurityClearance securityClearance) {
        this.securityClearance = securityClearance;
    }

    public String getThumbprint() {
        return thumbprint;
    }

    public void setThumbprint(String thumbprint) {
        this.thumbprint = thumbprint;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Map<String, DatabaseAccess> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, DatabaseAccess> permissions) {
        this.permissions = permissions;
    }

    public String getCollectionPrimaryKey() {
        return collectionPrimaryKey;
    }

    public void setCollectionPrimaryKey(String collectionPrimaryKey) {
        this.collectionPrimaryKey = collectionPrimaryKey;
    }

    public List<String> getCollectionSecondaryKeys() {
        return collectionSecondaryKeys;
    }

    public void setCollectionSecondaryKeys(List<String> collectionSecondaryKeys) {
        this.collectionSecondaryKeys = collectionSecondaryKeys;
    }

    public String getPublicKeyPinningHash() {
        return publicKeyPinningHash;
    }

    public void setPublicKeyPinningHash(String publicKeyPinningHash) {
        this.publicKeyPinningHash = publicKeyPinningHash;
    }
}
