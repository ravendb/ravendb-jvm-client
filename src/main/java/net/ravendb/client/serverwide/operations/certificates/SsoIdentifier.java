package net.ravendb.client.serverwide.operations.certificates;

/**
 * Identifies an SSO principal that a client certificate authorizes.
 */
public final class SsoIdentifier {

    private SsoProvider provider;
    private String domain;
    private String identifier;

    public SsoProvider getProvider() {
        return provider;
    }

    public void setProvider(SsoProvider provider) {
        this.provider = provider;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
