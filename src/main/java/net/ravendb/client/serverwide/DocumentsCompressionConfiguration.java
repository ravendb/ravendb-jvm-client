package net.ravendb.client.serverwide;

public class DocumentsCompressionConfiguration {
    private String[] collections;
    private boolean compressAllCollections;
    private boolean compressRevisions;

    public DocumentsCompressionConfiguration() {
    }

    public DocumentsCompressionConfiguration(boolean compressRevisions, String... collections) {
        this.compressRevisions = compressRevisions;
        this.collections = collections;
    }

    public DocumentsCompressionConfiguration(boolean compressRevisions, boolean compressAllCollections, String... collections) {
        this.compressRevisions = compressRevisions;
        this.compressAllCollections = compressAllCollections;
        this.collections = collections;
    }

    public String[] getCollections() {
        return collections;
    }

    public void setCollections(String[] collections) {
        this.collections = collections;
    }

    public boolean isCompressAllCollections() {
        return compressAllCollections;
    }

    public void setCompressAllCollections(boolean compressAllCollections) {
        this.compressAllCollections = compressAllCollections;
    }

    public boolean isCompressRevisions() {
        return compressRevisions;
    }

    public void setCompressRevisions(boolean compressRevisions) {
        this.compressRevisions = compressRevisions;
    }
}
