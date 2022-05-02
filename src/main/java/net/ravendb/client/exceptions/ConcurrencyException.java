package net.ravendb.client.exceptions;

public class ConcurrencyException extends ConflictException {
    private long expectedETag;

    private long actualETag;

    private String expectedChangeVector;

    private String actualChangeVector;

    private String id;

    public String getExpectedChangeVector() {
        return expectedChangeVector;
    }

    public void setExpectedChangeVector(String expectedChangeVector) {
        this.expectedChangeVector = expectedChangeVector;
    }

    public String getActualChangeVector() {
        return actualChangeVector;
    }

    public void setActualChangeVector(String actualChangeVector) {
        this.actualChangeVector = actualChangeVector;
    }

    /**
     * @deprecated Not used and will be removed and the next major version
     * @return expected etag
     */
    public long getExpectedETag() {
        return expectedETag;
    }

    /**
     * @deprecated Not used and will be removed and the next major version
     * @param expectedETag expected etag
     */
    public void setExpectedETag(long expectedETag) {
        this.expectedETag = expectedETag;
    }

    /**
     * Not used and will be removed and the next major version
     * @return actual etag
     */
    public long getActualETag() {
        return actualETag;
    }

    /**
     * @deprecated Not used and will be removed and the next major version
     * @param actualETag actual etag
     */
    public void setActualETag(long actualETag) {
        this.actualETag = actualETag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
