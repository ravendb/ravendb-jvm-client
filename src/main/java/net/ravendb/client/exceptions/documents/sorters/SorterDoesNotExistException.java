package net.ravendb.client.exceptions.documents.sorters;

import net.ravendb.client.exceptions.RavenException;

public class SorterDoesNotExistException extends RavenException {
    public SorterDoesNotExistException() {
    }

    public SorterDoesNotExistException(String message) {
        super(message);
    }

    public SorterDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SorterDoesNotExistException throwFor(String sorterName) {
        throw new SorterDoesNotExistException("There is no sorter with '" + sorterName + "' name.");
    }
}
