package net.ravendb.client.serverwide.operations.certificates;

import java.util.*;

public class CertificateDefinition {

    private String name;
    private String certificate;
    private String password;
    private SecurityClearance securityClearance;
    private String thumbprint;
    private Date notAfter;
    private Map<String, DatabaseAccess> permissions = new TreeMap<>(String::compareToIgnoreCase);
    private String collectionPrimaryKey = "";
    private List<String> collectionSecondaryKeys = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
}
