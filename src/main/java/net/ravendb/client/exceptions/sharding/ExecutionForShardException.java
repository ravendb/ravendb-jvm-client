package net.ravendb.client.exceptions.sharding;

import net.ravendb.client.exceptions.RavenException;

public class ExecutionForShardException extends RavenException {

    public ExecutionForShardException(int shardNumber, Exception inner) {
        super(getMessage(shardNumber), inner);
    }

    private static String getMessage(int shardNumber){
        return "Shard " + shardNumber + " execution failed with an exception.";
    }
}
