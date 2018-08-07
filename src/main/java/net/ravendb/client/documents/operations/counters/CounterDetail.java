package net.ravendb.client.documents.operations.counters;

import java.util.Map;

public class CounterDetail {
    private String documentId;
    private String counterName;
    private long totalValue;
    private long etag;
    private Map<String, Long> counterValues;

    private String changeVector;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public long getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(long totalValue) {
        this.totalValue = totalValue;
    }

    public long getEtag() {
        return etag;
    }

    public void setEtag(long etag) {
        this.etag = etag;
    }

    public Map<String, Long> getCounterValues() {
        return counterValues;
    }

    public void setCounterValues(Map<String, Long> counterValues) {
        this.counterValues = counterValues;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }
}