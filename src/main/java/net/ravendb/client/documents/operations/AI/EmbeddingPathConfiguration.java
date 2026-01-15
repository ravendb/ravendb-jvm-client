package net.ravendb.client.documents.operations.AI;

public class EmbeddingPathConfiguration {

    private String path;
    private ChunkingOptions chunkingOptions;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ChunkingOptions getChunkingOptions() {
        return chunkingOptions;
    }

    public void setChunkingOptions(ChunkingOptions chunkingOptions) {
        this.chunkingOptions = chunkingOptions;
    }

    static boolean areEqual(EmbeddingPathConfiguration left, EmbeddingPathConfiguration right) {
        if (left == null && right == null)
            return true;

        if (left == null || right == null)
            return false;

        boolean samePath = (left.path == null && right.path == null)
                || (left.path != null && left.path.equals(right.path));

        boolean sameChunking = ChunkingOptions.areEqual(
                left.chunkingOptions,
                right.chunkingOptions
        );

        return samePath && sameChunking;
    }
}
