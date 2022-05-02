package net.ravendb.client.documents.indexes;

public abstract class IndexDefinitionBase {

    private String name;
    private IndexPriority priority;
    private IndexState state;

    /**
     * This is the means by which the outside world refers to this index definition
     * @return index name
     */
    public String getName() {
        return name;
    }

    /**
     * This is the means by which the outside world refers to this index definition
     * @param name sets the value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Priority of an index
     * @return index priority
     */
    public IndexPriority getPriority() {
        return priority;
    }

    /**
     * Priority of an index
     * @param priority Sets the value
     */
    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    /**
     * State of an index
     * @return index state
     */
    public IndexState getState() {
        return state;
    }

    /**
     * State of an index
     * @param state index state
     */
    public void setState(IndexState state) {
        this.state = state;
    }

}
