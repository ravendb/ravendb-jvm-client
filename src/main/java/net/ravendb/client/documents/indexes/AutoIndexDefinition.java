package net.ravendb.client.documents.indexes;

import java.util.Map;

public class AutoIndexDefinition {
    private IndexType indexType;
    private String name;
    private IndexPriority priority;
    private IndexState state;
    private String collection;
    private Map<String, AutoIndexFieldOptions> mapFields;
    private Map<String, AutoIndexFieldOptions> groupByFields;

    public IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(IndexType indexType) {
        this.indexType = indexType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IndexPriority getPriority() {
        return priority;
    }

    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    public IndexState getState() {
        return state;
    }

    public void setState(IndexState state) {
        this.state = state;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Map<String, AutoIndexFieldOptions> getMapFields() {
        return mapFields;
    }

    public void setMapFields(Map<String, AutoIndexFieldOptions> mapFields) {
        this.mapFields = mapFields;
    }

    public Map<String, AutoIndexFieldOptions> getGroupByFields() {
        return groupByFields;
    }

    public void setGroupByFields(Map<String, AutoIndexFieldOptions> groupByFields) {
        this.groupByFields = groupByFields;
    }
}
