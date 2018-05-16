package net.ravendb.client.documents.changes;

public class IndexChange extends DatabaseChange {
    private IndexChangeTypes type;
    private String name;

    /**
     * @return Type of change that occurred on index.
     */
    public IndexChangeTypes getType() {
        return type;
    }

    /**
     * @param type Type of change that occurred on index.
     */
    public void setType(IndexChangeTypes type) {
        this.type = type;
    }

    /**
     * @return Name of index for which notification was created
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Name of index for which notification was created
     */
    public void setName(String name) {
        this.name = name;
    }
}
