package net.ravendb.client.documents.commands.batches;

/**
 * The result of a PUT operation
 */
public class PutResult {
    private String id;
    private String changeVector;

    /**
     * Id of the document that was PUT.
     * @return Id of document
     */
    public String getId() {
        return id;
    }

    /**
     * Id of the document that was PUT.
     * @param id Id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Change Vector of the document after PUT operation.
     * @return Change vector
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * Change Vector of the document after PUT operation.
     * @param changeVector Change vector to set
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }
}
