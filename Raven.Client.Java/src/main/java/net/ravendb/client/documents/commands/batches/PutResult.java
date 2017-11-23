package net.ravendb.client.documents.commands.batches;

/**
 * The result of a PUT operation
 */
public class PutResult {
    private String id;
    private String changeVector;

    /**
     * Id of the document that was PUT.
     */
    public String getId() {
        return id;
    }

    /**
     * Id of the document that was PUT.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Change Vector of the document after PUT operation.
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * Change Vector of the document after PUT operation.
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }
}
