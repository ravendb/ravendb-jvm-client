package net.ravendb.client.exceptions;

import java.time.Duration;

public final class RateLimitException extends TooManyRequestsException {

    private Duration retryAfter;

    public RateLimitException(String message) {
        super(message);
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(Duration retryAfter) {
        this.retryAfter = retryAfter;
    }
}
