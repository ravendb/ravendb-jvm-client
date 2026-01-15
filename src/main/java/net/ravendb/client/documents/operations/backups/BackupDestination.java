package net.ravendb.client.documents.operations.backups;

public enum BackupDestination {
    NONE("None"),
    LOCAL("Local"),
    AMAZON_S3("Amazon S3"),
    AMAZON_GLACIER("Amazon Glacier"),
    AZURE("Azure"),
    GOOGLE_CLOUD("Google Cloud"),
    FTP("FTP");

    private final String description;

    BackupDestination(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

