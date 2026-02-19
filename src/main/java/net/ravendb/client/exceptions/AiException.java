package net.ravendb.client.exceptions;

public class AiException extends RavenException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Exception e) {
        super(message, e);
    }

    private String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
