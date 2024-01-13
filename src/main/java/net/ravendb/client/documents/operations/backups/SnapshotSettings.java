package net.ravendb.client.documents.operations.backups;

public class SnapshotSettings {
    private SnapshotBackupCompressionAlgorithm compressionAlgorithm;
    private CompressionLevel compressionLevel;
    private boolean excludeIndexes;

    public SnapshotBackupCompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(SnapshotBackupCompressionAlgorithm compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }

    public CompressionLevel getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(CompressionLevel compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public boolean isExcludeIndexes() {
        return excludeIndexes;
    }

    public void setExcludeIndexes(boolean excludeIndexes) {
        this.excludeIndexes = excludeIndexes;
    }
}
