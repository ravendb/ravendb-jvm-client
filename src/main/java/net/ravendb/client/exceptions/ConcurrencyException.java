package net.ravendb.client.exceptions;

public class ConcurrencyException extends ConflictException {

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
