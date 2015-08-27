package net.ravendb.abstractions.data;

import net.ravendb.abstractions.indexing.IndexDefinition;

public class IndexToAdd {
    private String name;
    private IndexDefinition definition;
    private IndexStats.IndexingPriority priority;

    /**
     *  The name of an index that will be added
     */
    public String getName() {
        return name;
    }

    /**
     *  The name of an index that will be added
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Definition of an index
     */
    public IndexDefinition getDefinition() {
        return definition;
    }

    /**
     * Definition of an index
     */
    public void setDefinition(IndexDefinition definition) {
        this.definition = definition;
    }

    /**
     * Priority of an index
     */
    public IndexStats.IndexingPriority getPriority() {
        return priority;
    }

    /**
     * Priority of an index
     */
    public void setPriority(IndexStats.IndexingPriority priority) {
        this.priority = priority;
    }
}
