package net.ravendb.client.primitives;

public class OperationCancelledException extends RuntimeException {

    public OperationCancelledException() {
        super();
    }

    public OperationCancelledException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationCancelledException(String message) {
        super(message);
    }

    public OperationCancelledException(Throwable cause) {
        super(cause);
    }

}
