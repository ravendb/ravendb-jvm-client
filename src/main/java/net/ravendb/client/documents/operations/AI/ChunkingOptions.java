package net.ravendb.client.documents.operations.AI;

public class ChunkingOptions {

    private ChunkingMethod chunkingMethod;
    private int maxTokensPerChunk = 512;
    private int overlapTokens = 0;
    private String contextPrefix;

    public ChunkingOptions() {
    }

    public ChunkingOptions(ChunkingMethod chunkingMethod) {
        this.chunkingMethod = chunkingMethod;
    }

    public ChunkingOptions(ChunkingMethod chunkingMethod, int maxTokensPerChunk) {
        this.chunkingMethod = chunkingMethod;
        this.maxTokensPerChunk = maxTokensPerChunk;
    }

    public ChunkingOptions(ChunkingMethod chunkingMethod, int maxTokensPerChunk, int overlapTokens) {
        this.chunkingMethod = chunkingMethod;
        this.maxTokensPerChunk = maxTokensPerChunk;
        this.overlapTokens = overlapTokens;
    }

    public ChunkingMethod getChunkingMethod() {
        return chunkingMethod;
    }

    public void setChunkingMethod(ChunkingMethod chunkingMethod) {
        this.chunkingMethod = chunkingMethod;
    }

    public int getMaxTokensPerChunk() {
        return maxTokensPerChunk;
    }

    public void setMaxTokensPerChunk(int maxTokensPerChunk) {
        this.maxTokensPerChunk = maxTokensPerChunk;
    }

    public int getOverlapTokens() {
        return overlapTokens;
    }

    public void setOverlapTokens(int overlapTokens) {
        this.overlapTokens = overlapTokens;
    }

    /**
     * Optional constant text prepended to every produced chunk before it is sent to the embedding model.
     * Useful for adding broader document context (e.g. title) to isolated chunks.
     * The prefix's tokens count against {@link #getMaxTokensPerChunk()} - the effective chunking budget is reduced accordingly.
     * Trailing whitespace in the prefix is trimmed and a single space is automatically inserted between the prefix and each chunk's content.
     * @return Context prefix
     */
    public String getContextPrefix() {
        return contextPrefix;
    }

    public void setContextPrefix(String contextPrefix) {
        this.contextPrefix = contextPrefix;
    }
}
