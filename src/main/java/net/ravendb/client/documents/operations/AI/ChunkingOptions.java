package net.ravendb.client.documents.operations.AI;

public class ChunkingOptions {

    private ChunkingMethod chunkingMethod;
    private int maxTokensPerChunk = 512;
    private int overlapTokens = 0;

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
}
