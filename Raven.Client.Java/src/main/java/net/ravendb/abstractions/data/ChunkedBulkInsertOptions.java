package net.ravendb.abstractions.data;

/**
 * Options for the chunked bulk insert operation
 */
public class ChunkedBulkInsertOptions {

    private int maxDocumentsPerChunk;
    private long maxChunkVolumeInBytes;

    public ChunkedBulkInsertOptions() {
        this.maxDocumentsPerChunk = 2048;
        this.maxChunkVolumeInBytes = 8 * 1024 * 1024;
    }

    /**
     * Number of documents to send in each bulk insert sub operation (Default: 2048)
     */
    public int getMaxDocumentsPerChunk() {
        return maxDocumentsPerChunk;
    }

    /**
     * Number of documents to send in each bulk insert sub operation (Default: 2048)
     */
    public void setMaxDocumentsPerChunk(int maxDocumentsPerChunk) {
        this.maxDocumentsPerChunk = maxDocumentsPerChunk;
    }

    /**
     * Max volume of all the documents could be sent in each bulk insert sub operation (Default: 8MB)
     */
    public long getMaxChunkVolumeInBytes() {
        return maxChunkVolumeInBytes;
    }

    /**
     * Max volume of all the documents could be sent in each bulk insert sub operation (Default: 8MB)
     */
    public void setMaxChunkVolumeInBytes(long maxChunkVolumeInBytes) {
        this.maxChunkVolumeInBytes = maxChunkVolumeInBytes;
    }
}
