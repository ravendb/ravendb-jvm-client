package net.ravendb.client.documents.operations.backups;

public class SnapshotSettings {
    private CompressionLevel compressionLevel;

    public CompressionLevel getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(CompressionLevel compressionLevel) {
        this.compressionLevel = compressionLevel;
    }
}
