package net.ravendb.client.serverwide;

public class CompactSettings {
    private String databaseName;
    private boolean documents;
    private String[] indexes;
    private boolean skipOptimizeIndexes;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean isDocuments() {
        return documents;
    }

    public void setDocuments(boolean documents) {
        this.documents = documents;
    }

    public String[] getIndexes() {
        return indexes;
    }

    public void setIndexes(String[] indexes) {
        this.indexes = indexes;
    }

    public boolean isSkipOptimizeIndexes() {
        return skipOptimizeIndexes;
    }

    public void setSkipOptimizeIndexes(boolean skipOptimizeIndexes) {
        this.skipOptimizeIndexes = skipOptimizeIndexes;
    }
}
