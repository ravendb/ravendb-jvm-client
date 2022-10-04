package net.ravendb.client.exceptions.corax;

import net.ravendb.client.exceptions.RavenException;

public class NotSupportedInCoraxException extends RavenException {
    public NotSupportedInCoraxException() {
    }

    public NotSupportedInCoraxException(String message) {
        super(message);
    }

    public NotSupportedInCoraxException(String message, Throwable cause) {
        super(message, cause);
    }
}
