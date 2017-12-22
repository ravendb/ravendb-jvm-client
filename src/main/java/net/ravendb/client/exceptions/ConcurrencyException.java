package net.ravendb.client.exceptions;

public class ConcurrencyException extends ConflictException {
    private long expectedETag;

    private long actualETag;

    public long getExpectedETag() {
        return expectedETag;
    }

    public void setExpectedETag(long expectedETag) {
        this.expectedETag = expectedETag;
    }

    public long getActualETag() {
        return actualETag;
    }

    public void setActualETag(long actualETag) {
        this.actualETag = actualETag;
    }

    public ConcurrencyException() {
    }

    public ConcurrencyException(String message) {
        super(message);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
