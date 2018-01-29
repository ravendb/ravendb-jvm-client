package net.ravendb.client.documents.operations.configuration;

import net.ravendb.client.http.ReadBalanceBehavior;

public class ClientConfiguration {

    private long etag;
    private boolean disabled;
    private Integer maxNumberOfRequestsPerSession;
    private ReadBalanceBehavior readBalanceBehavior;

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
}
