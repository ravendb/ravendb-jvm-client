package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.indexes.spatial.AutoSpatialOptions;

public class AutoIndexFieldOptions {
    private FieldStorage storage;
    private AutoFieldIndexing indexing;
    private AggregationOperation aggregation;
    private AutoSpatialOptions spatial;
    private GroupByArrayBehavior groupByArrayBehavior;
    private Boolean suggestions;
    private boolean isNameQuoted;

    public FieldStorage getStorage() {
        return storage;
    }

    public void setStorage(FieldStorage storage) {
        this.storage = storage;
    }

    public AutoFieldIndexing getIndexing() {
        return indexing;
    }

    public void setIndexing(AutoFieldIndexing indexing) {
        this.indexing = indexing;
    }

    public AggregationOperation getAggregation() {
        return aggregation;
    }

    public void setAggregation(AggregationOperation aggregation) {
        this.aggregation = aggregation;
    }

    public AutoSpatialOptions getSpatial() {
        return spatial;
    }

    public void setSpatial(AutoSpatialOptions spatial) {
        this.spatial = spatial;
    }

    public GroupByArrayBehavior getGroupByArrayBehavior() {
        return groupByArrayBehavior;
    }

    public void setGroupByArrayBehavior(GroupByArrayBehavior groupByArrayBehavior) {
        this.groupByArrayBehavior = groupByArrayBehavior;
    }

    public Boolean getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(Boolean suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isNameQuoted() {
        return isNameQuoted;
    }

    public void setNameQuoted(boolean nameQuoted) {
        isNameQuoted = nameQuoted;
    }
}
