package net.ravendb.client.exceptions.documents.counters;

import net.ravendb.client.exceptions.RavenException;

public class CounterOverflowException extends RavenException {
    public CounterOverflowException() {
    }

    public CounterOverflowException(String message) {
        super(message);
    }

    public CounterOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
