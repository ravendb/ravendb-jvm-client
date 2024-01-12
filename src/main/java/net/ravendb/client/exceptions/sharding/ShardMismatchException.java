package net.ravendb.client.exceptions.sharding;

import net.ravendb.client.exceptions.RavenException;

public class ShardMismatchException extends RavenException {
    public ShardMismatchException() {
    }

    public ShardMismatchException(String message) {
        super(message);
    }

    public ShardMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
