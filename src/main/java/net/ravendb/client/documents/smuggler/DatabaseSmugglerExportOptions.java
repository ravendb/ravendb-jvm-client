package net.ravendb.client.documents.smuggler;

public class DatabaseSmugglerExportOptions extends DatabaseSmugglerOptions implements IDatabaseSmugglerExportOptions {
    private ExportCompressionAlgorithm compressionAlgorithm;

    public ExportCompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(ExportCompressionAlgorithm compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }
}
