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
    private boolean disabled;
    private CertificateUsage usage;
    private List<String> ssoServerPublicKeyPinningHashes = new ArrayList<>();
    private boolean allowAnySsoServer;
    private List<SsoIdentifier> ssoIdentifiers = new ArrayList<>();

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

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return what this certificate is used for, or null when the server did not classify it
     */
    public CertificateUsage getUsage() {
        return usage;
    }

    public void setUsage(CertificateUsage usage) {
        this.usage = usage;
    }

    /**
     * @return the public key pinning hashes of the SSO servers allowed to authorize this certificate's user
     */
    public List<String> getSsoServerPublicKeyPinningHashes() {
        return ssoServerPublicKeyPinningHashes;
    }

    public void setSsoServerPublicKeyPinningHashes(List<String> ssoServerPublicKeyPinningHashes) {
        this.ssoServerPublicKeyPinningHashes = ssoServerPublicKeyPinningHashes;
    }

    /**
     * @return true when any SSO server may authorize this certificate's user, ignoring
     *         {@link #getSsoServerPublicKeyPinningHashes()}
     */
    public boolean isAllowAnySsoServer() {
        return allowAnySsoServer;
    }

    public void setAllowAnySsoServer(boolean allowAnySsoServer) {
        this.allowAnySsoServer = allowAnySsoServer;
    }

    /**
     * @return the SSO principals this certificate authorizes
     */
    public List<SsoIdentifier> getSsoIdentifiers() {
        return ssoIdentifiers;
    }

    public void setSsoIdentifiers(List<SsoIdentifier> ssoIdentifiers) {
        this.ssoIdentifiers = ssoIdentifiers;
    }
}
