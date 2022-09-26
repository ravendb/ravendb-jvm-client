package net.ravendb.client.exceptions.commercial;

import net.ravendb.client.exceptions.RavenException;

public class PortInUseException extends RavenException {
    public PortInUseException() {
    }

    public PortInUseException(String message) {
        super(message);
    }

    public PortInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
