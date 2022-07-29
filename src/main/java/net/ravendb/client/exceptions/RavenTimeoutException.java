package net.ravendb.client.exceptions;

public class RavenTimeoutException extends RavenException {

    private boolean failImmediately;

    public RavenTimeoutException() {
    }

    public RavenTimeoutException(String message) {
        super(message);
    }

    public RavenTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public boolean isFailImmediately() {
        return failImmediately;
    }

    public void setFailImmediately(boolean failImmediately) {
        this.failImmediately = failImmediately;
    }
}
