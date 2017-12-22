package net.ravendb.client.documents.session;

public class SessionInfo {

    private final Integer sessionId;

    public SessionInfo(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getSessionId() {
        return sessionId;
    }
}
