package net.ravendb.client.documents.operations.counters;

import java.util.Map;

/**
 * Represents detailed information about a counter associated with a document in RavenDB.
 */
public class CounterDetail {
    /**
     * Gets or sets the ID of the document to which the counter belongs.
     */

    private String documentId;
    /**
     * Gets or sets the name of the counter.
     *
     * <p>This identifies the specific counter within the document.</p>
     */
    private String counterName;
    /**
     * Gets or sets the total value of the counter across all nodes.
     *
     * <p>This value is the aggregate of all per-node counter values stored in {@link #counterValues}.</p>
     */
    private long totalValue;
    /**
     * Gets or sets the ETag associated with the counter.
     *
     * <p>The ETag is a unique identifier used to track changes to the counter.</p>
     */
    private long etag;
    /**
     * Gets or sets the dictionary of counter values for each node in the cluster.
     *
     * <p>The key is the node identifier, and the value is the counter value for that node.</p>
     */
    private Map<String, Long> counterValues;
    /**
     * Gets or sets the change vector for the counter.
     *
     * <p>The change vector represents the version history of the counter and is used for concurrency control.</p>
     */
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