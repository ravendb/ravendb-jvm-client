package net.ravendb.client.documents.changes;

public class DocumentChange extends DatabaseChange {
    private DocumentChangeTypes type;

    private String id;

    private String collectionName;

    private String changeVector;

    /**
     * @return  Type of change that occurred on document.
     */
    public DocumentChangeTypes getType() {
        return type;
    }

    /**
     * @param type Type of change that occurred on document.
     */
    public void setType(DocumentChangeTypes type) {
        this.type = type;
    }

    /**
     * @return Identifier of document for which notification was created.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id Identifier of document for which notification was created.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Document collection name.
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @param collectionName Document collection name.
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * @return Document change vector
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * @param changeVector Document change vector
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    @Override
    public String toString() {
        return type + " on " + id;
    }
}
