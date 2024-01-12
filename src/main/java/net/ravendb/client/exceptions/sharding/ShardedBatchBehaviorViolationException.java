package net.ravendb.client.exceptions.sharding;

import net.ravendb.client.exceptions.RavenException;

public class ShardedBatchBehaviorViolationException extends RavenException {
    public ShardedBatchBehaviorViolationException() {
    }

    public ShardedBatchBehaviorViolationException(String message) {
        super(message);
    }

    public ShardedBatchBehaviorViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
