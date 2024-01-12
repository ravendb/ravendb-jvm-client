package net.ravendb.client.exceptions.sharding;

import net.ravendb.client.exceptions.RavenException;

public class NotSupportedInShardingException extends RavenException {
    public NotSupportedInShardingException() {
    }

    public NotSupportedInShardingException(String message) {
        super(message);
    }

    public NotSupportedInShardingException(String message, Throwable cause) {
        super(message, cause);
    }
}
