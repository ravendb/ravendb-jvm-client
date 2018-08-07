package net.ravendb.client.documents.session;

public class SessionInfo {

    private Long lastClusterTransactionIndex;
    private final Integer sessionId;
    private boolean noCaching;

    public SessionInfo(Integer sessionId) {
        this(sessionId, null, false);
    }

    public SessionInfo(Integer sessionId, Long lastClusterTransactionIndex, boolean noCaching) {
        this.sessionId = sessionId;
        this.lastClusterTransactionIndex = lastClusterTransactionIndex;
        this.noCaching = noCaching;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public Long getLastClusterTransactionIndex() {
        return lastClusterTransactionIndex;
    }

    public void setLastClusterTransactionIndex(Long lastClusterTransactionIndex) {
        this.lastClusterTransactionIndex = lastClusterTransactionIndex;
    }

    public boolean isNoCaching() {
        return noCaching;
    }

    public void setNoCaching(boolean noCaching) {
        this.noCaching = noCaching;
    }


}
