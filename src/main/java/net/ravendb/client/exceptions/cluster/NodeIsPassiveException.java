package net.ravendb.client.exceptions.cluster;

import net.ravendb.client.exceptions.RavenException;

public class NodeIsPassiveException extends RavenException {
    public NodeIsPassiveException() {
    }

    public NodeIsPassiveException(String message) {
        super(message);
    }

    public NodeIsPassiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
