package net.ravendb.client.exceptions;

public final class RateLimitException extends TooManyRequestsException {

    private java.time.Duration retryAfter;

    public RateLimitException(String message) {
        super(message);
    }

    public java.time.Duration getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(java.time.Duration retryAfter) {
        this.retryAfter = retryAfter;
    }
}
