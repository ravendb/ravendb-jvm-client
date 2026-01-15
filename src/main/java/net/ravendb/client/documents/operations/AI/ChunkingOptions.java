package net.ravendb.client.documents.operations.AI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkingOptions {

    private ChunkingMethod chunkingMethod;
    private int maxTokensPerChunk = 512;
    private int overlapTokens = 0;

    static final Set<ChunkingMethod> METHODS_SUPPORTING_OVERLAP_TOKENS =
            new HashSet<>(Arrays.asList(
                    ChunkingMethod.MARK_DOWN_SPLIT_PARAGRAPHS,
                    ChunkingMethod.PLAIN_TEXT_SPLIT_PARAGRAPHS
            ));

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

    void validate(String source, List<String> errors) {

        if (maxTokensPerChunk <= 0) {
            errors.add("'" + source + "': MaxTokensPerChunk value has to be greater than 0.");
        }

        if (overlapTokens < 0) {
            errors.add("'" + source + "': OverlapTokens value cannot be negative.");
        }

        if (overlapTokens > maxTokensPerChunk) {
            errors.add("'" + source + "': OverlapTokens cannot be greater than MaxTokensPerChunk.");
        }

        if (overlapTokens > 0 &&
                !METHODS_SUPPORTING_OVERLAP_TOKENS.contains(chunkingMethod)) {

            errors.add("'" + source + "': OverlapTokens option is only supported for the following chunking methods: "
                    + METHODS_SUPPORTING_OVERLAP_TOKENS);
        }
    }

    static boolean areEqual(ChunkingOptions left, ChunkingOptions right) {
        if (left == null && right == null)
            return true;

        if (left == null || right == null)
            return false;

        boolean sameMethod = left.chunkingMethod == right.chunkingMethod;
        boolean sameMax = left.maxTokensPerChunk == right.maxTokensPerChunk;
        boolean sameOverlap = left.overlapTokens == right.overlapTokens;

        return sameMethod && sameMax && sameOverlap;
    }
}
