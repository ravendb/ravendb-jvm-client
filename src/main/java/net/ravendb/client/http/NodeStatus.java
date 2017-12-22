package net.ravendb.client.http;

import java.util.Date;

public class NodeStatus {

    private boolean connected;
    private String errorDetails;
    private Date lastSend;
    private Date lastReply;
    private String lastSentMessage;
    private long lastMatchingIndex;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Date getLastSend() {
        return lastSend;
    }

    public void setLastSend(Date lastSend) {
        this.lastSend = lastSend;
    }

    public Date getLastReply() {
        return lastReply;
    }

    public void setLastReply(Date lastReply) {
        this.lastReply = lastReply;
    }

    public String getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(String lastSentMessage) {
        this.lastSentMessage = lastSentMessage;
    }

    public long getLastMatchingIndex() {
        return lastMatchingIndex;
    }

    public void setLastMatchingIndex(long lastMatchingIndex) {
        this.lastMatchingIndex = lastMatchingIndex;
    }
}
