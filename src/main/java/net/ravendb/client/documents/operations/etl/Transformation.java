package net.ravendb.client.documents.operations.etl;

import java.util.*;

public class Transformation {
    private String name;
    private boolean disabled;
    private List<String> collections = new ArrayList<>();
    private boolean applyToAllDocuments;
    private String script;

    public Transformation() {
        this.counters = new CountersTransformation(this);
        this.timeSeries = new TimeSeriesTransformation(this);
    }

    private String documentIdPostfix;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public boolean isApplyToAllDocuments() {
        return applyToAllDocuments;
    }

    public void setApplyToAllDocuments(boolean applyToAllDocuments) {
        this.applyToAllDocuments = applyToAllDocuments;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getDocumentIdPostfix() {
        return documentIdPostfix;
    }

    public void setDocumentIdPostfix(String documentIdPostfix) {
        this.documentIdPostfix = documentIdPostfix;
    }

    final CountersTransformation counters;

    public CountersTransformation getCounters() { return counters; }

    final class CountersTransformation {

        private final Transformation parent;
        boolean isAddingCounters;

        public CountersTransformation(Transformation parent) {
            this.parent = parent;
        }

        Map<String, String> collectionToLoadBehaviorFunction;

        public boolean isAddingCounters() {
            return isAddingCounters;
        }

        private void setAddingCounters(boolean value) {
            this.isAddingCounters = value;
        }

        public Map<String, String> getCollectionToLoadBehaviorFunction() {
            return collectionToLoadBehaviorFunction;
        }

        private void setCollectionToLoadBehaviorFunction(Map<String, String> value) {
            this.collectionToLoadBehaviorFunction = value;
        }
    }

    final TimeSeriesTransformation timeSeries;

    public TimeSeriesTransformation getTimeSeries() { return timeSeries; }

    static final class TimeSeriesTransformation {

        private final Transformation parent;
        boolean isAddingTimeSeries;
        Map<String, String> collectionToLoadBehaviorFunction;

        public boolean isAddingTimeSeries() {
            return isAddingTimeSeries;
        }

        private void setAddingTimeSeries(boolean value) {
            this.isAddingTimeSeries = value;
        }

        public Map<String, String> getCollectionToLoadBehaviorFunction() {
            return collectionToLoadBehaviorFunction;
        }

        private void setCollectionToLoadBehaviorFunction(Map<String, String> map) {
            this.collectionToLoadBehaviorFunction = map;
        }

        public TimeSeriesTransformation(Transformation parent) {
            this.parent = parent;
        }
    }
}
