package net.ravendb.client.documents.changes;

public class CounterChange extends DatabaseChange {
    private String name;
    private long value;
    private String documentId;
    private String changeVector;
    private CounterChangeTypes type;

    /**
     * @return Counter name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Counter name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Counter value.
     */
    public long getValue() {
        return value;
    }

    /**
     * @param value Counter value.
     */
    public void setValue(long value) {
        this.value = value;
    }

    /**
     * @return Counter document identifier.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @param documentId Counter document identifier.
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * @return Counter change vector.
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * @param changeVector Counter change vector.
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    /**
     * @return Type of change that occurred on counter.
     */
    public CounterChangeTypes getType() {
        return type;
    }

    /**
     * @param type Type of change that occurred on counter.
     */
    public void setType(CounterChangeTypes type) {
        this.type = type;
    }
}
