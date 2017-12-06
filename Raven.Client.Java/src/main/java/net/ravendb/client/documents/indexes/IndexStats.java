package net.ravendb.client.documents.indexes;

import java.util.Date;
import java.util.Map;

public class IndexStats {

    private String name;
    private int mapAttempts;
    private int mapSuccesses;
    private int mapErrors;
    private Integer reduceAttempts;
    private Integer reduceSuccesses;
    private Integer reduceErrors;
    private double mappedPerSecondRate;
    private double reducedPerSecondRate;
    private int maxNumberOfOutputsPerDocument;
    private Map<String, CollectionStats> collections;
    private Date lastQueryingTime;
    private IndexState state;
    private IndexPriority priority;
    private Date createdTimestamp;
    private Date lastIndexingTime;
    private boolean isStale; //TODO : check mapping
    private IndexLockMode lockMode;
    private IndexType type;
    private IndexRunningStatus status;
    private int entriesCount;
    private int errorsCount;
    private boolean isTestIndex;


    /**
     * Index name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indicates how many times database tried to index documents (map) using this index.
     */
    public int getMapAttempts() {
        return mapAttempts;
    }

    /**
     * Indicates how many times database tried to index documents (map) using this index.
     */
    public void setMapAttempts(int mapAttempts) {
        this.mapAttempts = mapAttempts;
    }

    /**
     * Indicates how many indexing attempts succeeded.
     */
    public int getMapSuccesses() {
        return mapSuccesses;
    }

    /**
     * Indicates how many indexing attempts succeeded.
     */
    public void setMapSuccesses(int mapSuccesses) {
        this.mapSuccesses = mapSuccesses;
    }

    /**
     * Indicates how many indexing attempts failed.
     */
    public int getMapErrors() {
        return mapErrors;
    }

    /**
     * Indicates how many indexing attempts failed.
     */
    public void setMapErrors(int mapErrors) {
        this.mapErrors = mapErrors;
    }

    /**
     * Indicates how many times database tried to index documents (reduce) using this index.
     */
    public Integer getReduceAttempts() {
        return reduceAttempts;
    }

    /**
     * Indicates how many times database tried to index documents (reduce) using this index.
     */
    public void setReduceAttempts(Integer reduceAttempts) {
        this.reduceAttempts = reduceAttempts;
    }

    /**
     * Indicates how many reducing attempts succeeded.
     */
    public Integer getReduceSuccesses() {
        return reduceSuccesses;
    }

    /**
     * Indicates how many reducing attempts succeeded.
     */
    public void setReduceSuccesses(Integer reduceSuccesses) {
        this.reduceSuccesses = reduceSuccesses;
    }

    /**
     * Indicates how many reducing attempts failed.
     */
    public Integer getReduceErrors() {
        return reduceErrors;
    }

    /**
     * Indicates how many reducing attempts failed.
     */
    public void setReduceErrors(Integer reduceErrors) {
        this.reduceErrors = reduceErrors;
    }

    /**
     * The value of docs/sec rate for the index over the last minute
     */
    public double getMappedPerSecondRate() {
        return mappedPerSecondRate;
    }

    /**
     * The value of docs/sec rate for the index over the last minute
     */
    public void setMappedPerSecondRate(double mappedPerSecondRate) {
        this.mappedPerSecondRate = mappedPerSecondRate;
    }

    /**
     * The value of reduces/sec rate for the index over the last minute
     */
    public double getReducedPerSecondRate() {
        return reducedPerSecondRate;
    }

    /**
     * The value of reduces/sec rate for the index over the last minute
     */
    public void setReducedPerSecondRate(double reducedPerSecondRate) {
        this.reducedPerSecondRate = reducedPerSecondRate;
    }

    /**
     * Indicates the maximum number of produced indexing outputs from a single document
     */
    public int getMaxNumberOfOutputsPerDocument() {
        return maxNumberOfOutputsPerDocument;
    }

    /**
     * Indicates the maximum number of produced indexing outputs from a single document
     */
    public void setMaxNumberOfOutputsPerDocument(int maxNumberOfOutputsPerDocument) {
        this.maxNumberOfOutputsPerDocument = maxNumberOfOutputsPerDocument;
    }

    public Map<String, CollectionStats> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, CollectionStats> collections) {
        this.collections = collections;
    }

    /**
     * Time of last query for this index.
     */
    public Date getLastQueryingTime() {
        return lastQueryingTime;
    }

    /**
     * Time of last query for this index.
     */
    public void setLastQueryingTime(Date lastQueryingTime) {
        this.lastQueryingTime = lastQueryingTime;
    }

    /**
     * Index state (Normal, Disabled, Idle, Abandoned, Error)
     */
    public IndexState getState() {
        return state;
    }

    /**
     * Index state (Normal, Disabled, Idle, Abandoned, Error)
     */
    public void setState(IndexState state) {
        this.state = state;
    }

    /**
     * Index priority (Low, Normal, High)
     */
    public IndexPriority getPriority() {
        return priority;
    }

    /**
     * Index priority (Low, Normal, High)param priority
     */
    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    /**
     * Date of index creation.
     */
    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Date of index creation.
     */
    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Time of last indexing (map or reduce) for this index.
     */
    public Date getLastIndexingTime() {
        return lastIndexingTime;
    }

    /**
     * Time of last indexing (map or reduce) for this index.
     */
    public void setLastIndexingTime(Date lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }

    public boolean isStale() {
        return isStale;
    }

    public void setStale(boolean stale) {
        isStale = stale;
    }

    /**
     * Indicates current lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     */
    public IndexLockMode getLockMode() {
        return lockMode;
    }

    /**
     * Indicates current lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     */
    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Indicates index type.
     */
    public IndexType getType() {
        return type;
    }

    /**
     * Indicates index type.
     */
    public void setType(IndexType type) {
        this.type = type;
    }

    public IndexRunningStatus getStatus() {
        return status;
    }

    public void setStatus(IndexRunningStatus status) {
        this.status = status;
    }

    /**
     * Total number of entries in this index.
     */
    public int getEntriesCount() {
        return entriesCount;
    }

    /**
     * Total number of entries in this index.
     */
    public void setEntriesCount(int entriesCount) {
        this.entriesCount = entriesCount;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    /**
     * Indicates if this is a test index (works on a limited data set - for testing purposes only)
     */
    public boolean isTestIndex() {
        return isTestIndex;
    }

    /**
     * Indicates if this is a test index (works on a limited data set - for testing purposes only)
     */
    public void setTestIndex(boolean testIndex) {
        isTestIndex = testIndex;
    }

    public static class CollectionStats {
        private long lastProcessedDocumentEtag;
        private long lastProcessedTombstoneEtag;
        private long documentLag;
        private long tombstoneLag;

        public CollectionStats() {
            documentLag = -1;
            tombstoneLag = -1;
        }

        public long getLastProcessedDocumentEtag() {
            return lastProcessedDocumentEtag;
        }

        public void setLastProcessedDocumentEtag(long lastProcessedDocumentEtag) {
            this.lastProcessedDocumentEtag = lastProcessedDocumentEtag;
        }

        public long getLastProcessedTombstoneEtag() {
            return lastProcessedTombstoneEtag;
        }

        public void setLastProcessedTombstoneEtag(long lastProcessedTombstoneEtag) {
            this.lastProcessedTombstoneEtag = lastProcessedTombstoneEtag;
        }

        public long getDocumentLag() {
            return documentLag;
        }

        public void setDocumentLag(long documentLag) {
            this.documentLag = documentLag;
        }

        public long getTombstoneLag() {
            return tombstoneLag;
        }

        public void setTombstoneLag(long tombstoneLag) {
            this.tombstoneLag = tombstoneLag;
        }
    }
}
