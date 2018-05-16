package net.ravendb.client.documents.changes;

public class IndexRenameChange extends IndexChange {
    private String oldIndexName;

    /**
     * @return The old index name
     */
    public String getOldIndexName() {
        return oldIndexName;
    }

    /**
     * @param oldIndexName The old index name
     */
    public void setOldIndexName(String oldIndexName) {
        this.oldIndexName = oldIndexName;
    }
}
