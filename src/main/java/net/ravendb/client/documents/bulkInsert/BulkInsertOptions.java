package net.ravendb.client.documents.bulkInsert;

import net.ravendb.client.documents.operations.backups.CompressionLevel;

public class BulkInsertOptions {

    public static final CompressionLevel DEFAULT_COMPRESSION_LEVEL = CompressionLevel.FASTEST;

    private CompressionLevel compressionLevel = DEFAULT_COMPRESSION_LEVEL;
    private boolean skipOverwriteIfUnchanged;

    /**
     * @return Compression level used for the bulk insert stream (NO_COMPRESSION disables compression). Default: FASTEST
     */
    public CompressionLevel getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * @param compressionLevel Compression level used for the bulk insert stream (NO_COMPRESSION disables compression). Default: FASTEST
     */
    public void setCompressionLevel(CompressionLevel compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * @return true if compression is enabled
     * @deprecated Bulk insert compression is now configured by compression level (enabled by default, as in the C# client). Use {@link #getCompressionLevel()} instead.
     */
    @Deprecated
    public boolean isUseCompression() {
        return compressionLevel != null && compressionLevel != CompressionLevel.NO_COMPRESSION;
    }

    /**
     * @param useCompression true sets compression level to FASTEST, false to NO_COMPRESSION
     * @deprecated Bulk insert compression is now configured by compression level (enabled by default, as in the C# client). Use {@link #setCompressionLevel(CompressionLevel)} instead.
     */
    @Deprecated
    public void setUseCompression(boolean useCompression) {
        this.compressionLevel = useCompression ? CompressionLevel.FASTEST : CompressionLevel.NO_COMPRESSION;
    }

    /**
     * Determines whether we should skip overwriting a document when it is updated by exactly the same document (by comparing the content and the metadata)
     * @return skip
     */
    public boolean isSkipOverwriteIfUnchanged() {
        return skipOverwriteIfUnchanged;
    }

    /**
     * Determines whether we should skip overwriting a document when it is updated by exactly the same document (by comparing the content and the metadata)
     * @param skipOverwriteIfUnchanged skip
     */
    public void setSkipOverwriteIfUnchanged(boolean skipOverwriteIfUnchanged) {
        this.skipOverwriteIfUnchanged = skipOverwriteIfUnchanged;
    }
}
