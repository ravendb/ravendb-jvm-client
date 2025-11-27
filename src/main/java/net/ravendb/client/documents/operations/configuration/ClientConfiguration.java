package net.ravendb.client.documents.operations.configuration;

import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.ReadBalanceBehavior;

/**
 * Represents the client configuration settings that control how the client communicates with the server.
 * <p>
 * This class includes options such as request limits, load balancing behaviors, and identity parts separator.
 * </p>
 */
public class ClientConfiguration {

    private Character identityPartsSeparator;
    /**
     * A version identifier for the configuration.
     */
    private long etag;
    /**
     * Indicates whether the client configuration is disabled.
     */
    private boolean disabled;
    /**
     * The maximum number of requests allowed per session.
     */
    private Integer maxNumberOfRequestsPerSession;
    /**
     * Specifies the read balance behavior to be used by the client.
     */
    private ReadBalanceBehavior readBalanceBehavior;
    /**
     * Specifies the load balance behavior to be used by the client.
     */
    private LoadBalanceBehavior loadBalanceBehavior;
    /**
     * A seed value used by the load balancer for distributing requests.
     */
    private Integer loadBalancerContextSeed;

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

    public Integer getLoadBalancerContextSeed() {
        return loadBalancerContextSeed;
    }

    public void setLoadBalancerContextSeed(Integer loadBalancerContextSeed) {
        this.loadBalancerContextSeed = loadBalancerContextSeed;
    }
}
