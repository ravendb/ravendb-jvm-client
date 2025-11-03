package net.ravendb.client.documents.AI;

import java.util.concurrent.CompletableFuture;

/**
 * Callback invoked with each streamed chunk from the AI agent response.
 * The callback can be synchronous or asynchronous.
 */
@FunctionalInterface
public interface AiStreamCallback {

    /**
     * Handle a streamed text chunk from the AI agent.
     * Implementations may process this synchronously or asynchronously.
     *
     * @param chunk The streamed text chunk from the specified property.
     */
    CompletableFuture<Void> onChunk(String chunk);
}