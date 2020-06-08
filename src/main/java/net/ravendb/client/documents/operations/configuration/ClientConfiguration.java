package net.ravendb.client.documents.operations.configuration;

import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.ReadBalanceBehavior;

public class ClientConfiguration {

    private Character identityPartsSeparator;
    private long etag;
    private boolean disabled;
    private Integer maxNumberOfRequestsPerSession;
    private ReadBalanceBehavior readBalanceBehavior;
    private LoadBalanceBehavior loadBalanceBehavior;

    public long getEtag() {
        return etag;
    }

    public void setEtag(long etag) {
        this.etag = etag;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getMaxNumberOfRequestsPerSession() {
        return maxNumberOfRequestsPerSession;
    }

    public void setMaxNumberOfRequestsPerSession(Integer maxNumberOfRequestsPerSession) {
        this.maxNumberOfRequestsPerSession = maxNumberOfRequestsPerSession;
    }

    public ReadBalanceBehavior getReadBalanceBehavior() {
        return readBalanceBehavior;
    }

    public void setReadBalanceBehavior(ReadBalanceBehavior readBalanceBehavior) {
        this.readBalanceBehavior = readBalanceBehavior;
    }

    public LoadBalanceBehavior getLoadBalanceBehavior() {
        return loadBalanceBehavior;
    }

    public void setLoadBalanceBehavior(LoadBalanceBehavior loadBalanceBehavior) {
        this.loadBalanceBehavior = loadBalanceBehavior;
    }

    public Character getIdentityPartsSeparator() {
        return identityPartsSeparator;
    }

    public void setIdentityPartsSeparator(Character identityPartsSeparator) {
        if (identityPartsSeparator != null && '|' == identityPartsSeparator) {
            throw new IllegalArgumentException("Cannot set identity parts separator to '|'");
        }
        this.identityPartsSeparator = identityPartsSeparator;
    }
}
