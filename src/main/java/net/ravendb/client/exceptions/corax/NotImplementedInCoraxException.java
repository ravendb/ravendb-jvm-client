package net.ravendb.client.exceptions.corax;

import net.ravendb.client.exceptions.RavenException;

public class NotImplementedInCoraxException extends RavenException {
    public NotImplementedInCoraxException() {
    }

    public NotImplementedInCoraxException(String message) {
        super(message);
    }

    public NotImplementedInCoraxException(String message, Throwable cause) {
        super(message, cause);
    }
}
