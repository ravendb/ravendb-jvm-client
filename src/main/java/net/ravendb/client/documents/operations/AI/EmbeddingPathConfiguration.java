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
}
