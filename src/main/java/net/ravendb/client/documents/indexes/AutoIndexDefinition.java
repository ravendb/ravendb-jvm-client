package net.ravendb.client.documents.indexes;

import java.util.List;
import java.util.Map;

public class AutoIndexDefinition extends IndexDefinitionBase {
    private IndexType type;
    private String collection;
    private Map<String, AutoIndexFieldOptions> mapFields;
    private Map<String, AutoIndexFieldOptions> groupByFields;
    private List<String> groupByFieldNames;

    public IndexType getType() {
        return type;
    }

    public void setType(IndexType type) {
        this.type = type;
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

    public List<String> getGroupByFieldNames() {
        return groupByFieldNames;
    }

    public void setGroupByFieldNames(List<String> groupByFieldNames) {
        this.groupByFieldNames = groupByFieldNames;
    }
}
