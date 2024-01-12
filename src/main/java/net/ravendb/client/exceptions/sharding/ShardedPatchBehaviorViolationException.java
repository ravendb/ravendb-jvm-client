package net.ravendb.client.exceptions.sharding;

import net.ravendb.client.exceptions.RavenException;

public class ShardedPatchBehaviorViolationException extends RavenException {
    public ShardedPatchBehaviorViolationException() {
    }

    public ShardedPatchBehaviorViolationException(String message) {
        super(message);
    }

    public ShardedPatchBehaviorViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
