package net.ravendb.client.documents.bulkInsert;

public class BulkInsertOptions {
    private boolean useCompression;
    private boolean skipOverwriteIfUnchanged;

    public boolean isUseCompression() {
        return useCompression;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
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
