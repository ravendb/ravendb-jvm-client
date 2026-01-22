package net.ravendb.client.documents.operations.AI;

public class EmbeddingsTransformation {

    private String script;
    private ChunkingOptions chunkingOptions =
            new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT, 256);

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public ChunkingOptions getChunkingOptions() {
        return chunkingOptions;
    }

    public void setChunkingOptions(ChunkingOptions chunkingOptions) {
        this.chunkingOptions = chunkingOptions;
    }
}
