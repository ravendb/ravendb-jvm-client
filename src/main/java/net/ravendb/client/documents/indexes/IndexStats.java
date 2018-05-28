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
    private boolean stale;
    private IndexLockMode lockMode;
    private IndexType type;
    private IndexRunningStatus status;
    private int entriesCount;
    private int errorsCount;
    private boolean isTestIndex;


    /**
     * Index name.
     * @return Index name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indicates how many times database tried to index documents (map) using this index.
     * @return map attempts
     */
    public int getMapAttempts() {
        return mapAttempts;
    }

    /**
     * Indicates how many times database tried to index documents (map) using this index.
     * @param mapAttempts Sets the value
     */
    public void setMapAttempts(int mapAttempts) {
        this.mapAttempts = mapAttempts;
    }

    /**
     * Indicates how many indexing attempts succeeded.
     * @return  map successes
     */
    public int getMapSuccesses() {
        return mapSuccesses;
    }

    /**
     * Indicates how many indexing attempts succeeded.
     * @param mapSuccesses Sets the value
     */
    public void setMapSuccesses(int mapSuccesses) {
        this.mapSuccesses = mapSuccesses;
    }

    /**
     * Indicates how many indexing attempts failed.
     * @return map errors
     */
    public int getMapErrors() {
        return mapErrors;
    }

    /**
     * Indicates how many indexing attempts failed.
     * @param mapErrors sets the value
     */
    public void setMapErrors(int mapErrors) {
        this.mapErrors = mapErrors;
    }

    /**
     * Indicates how many times database tried to index documents (reduce) using this index.
     * @return reduce attempts
     */
    public Integer getReduceAttempts() {
        return reduceAttempts;
    }

    /**
     * Indicates how many times database tried to index documents (reduce) using this index.
     * @param reduceAttempts sets the value
     */
    public void setReduceAttempts(Integer reduceAttempts) {
        this.reduceAttempts = reduceAttempts;
    }

    /**
     * Indicates how many reducing attempts succeeded.
     * @return reduce success count
     */
    public Integer getReduceSuccesses() {
        return reduceSuccesses;
    }

    /**
     * Indicates how many reducing attempts succeeded.
     * @param reduceSuccesses sets the value
     */
    public void setReduceSuccesses(Integer reduceSuccesses) {
        this.reduceSuccesses = reduceSuccesses;
    }

    /**
     * Indicates how many reducing attempts failed.
     * @return reduce errors
     */
    public Integer getReduceErrors() {
        return reduceErrors;
    }

    /**
     * Indicates how many reducing attempts failed.
     * @param reduceErrors Sets the value
     */
    public void setReduceErrors(Integer reduceErrors) {
        this.reduceErrors = reduceErrors;
    }

    /**
     * The value of docs/sec rate for the index over the last minute
     * @return amount of documents mapped/second
     */
    public double getMappedPerSecondRate() {
        return mappedPerSecondRate;
    }

    /**
     * The value of docs/sec rate for the index over the last minute
     * @param mappedPerSecondRate sets the value
     */
    public void setMappedPerSecondRate(double mappedPerSecondRate) {
        this.mappedPerSecondRate = mappedPerSecondRate;
    }

    /**
     * The value of reduces/sec rate for the index over the last minute
     * @return amount of documents reduced per second
     */
    public double getReducedPerSecondRate() {
        return reducedPerSecondRate;
    }

    /**
     * The value of reduces/sec rate for the index over the last minute
     * @param reducedPerSecondRate Sets the value
     */
    public void setReducedPerSecondRate(double reducedPerSecondRate) {
        this.reducedPerSecondRate = reducedPerSecondRate;
    }

    /**
     * Indicates the maximum number of produced indexing outputs from a single document
     * @return maximum number of outputs per document
     */
    public int getMaxNumberOfOutputsPerDocument() {
        return maxNumberOfOutputsPerDocument;
    }

    /**
     * Indicates the maximum number of produced indexing outputs from a single document
     * @param maxNumberOfOutputsPerDocument sets the value
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
     * @return Last query time for this index
     */
    public Date getLastQueryingTime() {
        return lastQueryingTime;
    }

    /**
     * Time of last query for this index.
     * @param lastQueryingTime Sets the value
     */
    public void setLastQueryingTime(Date lastQueryingTime) {
        this.lastQueryingTime = lastQueryingTime;
    }

    /**
     * Index state (Normal, Disabled, Idle, Abandoned, Error)
     * @return index state
     */
    public IndexState getState() {
        return state;
    }

    /**
     * Index state (Normal, Disabled, Idle, Abandoned, Error)
     * @param state Sets the value
     */
    public void setState(IndexState state) {
        this.state = state;
    }

    /**
     * Index priority (Low, Normal, High)
     * @return index priority
     */
    public IndexPriority getPriority() {
        return priority;
    }

    /**
     * Index priority (Low, Normal, High)param priority
     * @param priority sets the value
     */
    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    /**
     * Date of index creation.
     * @return Date of index creation
     */
    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    /**
     * Date of index creation.
     * @param createdTimestamp Sets the value
     */
    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Time of last indexing (map or reduce) for this index.
     * @return Time of last indexing
     */
    public Date getLastIndexingTime() {
        return lastIndexingTime;
    }

    /**
     * Time of last indexing (map or reduce) for this index.
     * @param lastIndexingTime Sets the value
     */
    public void setLastIndexingTime(Date lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    /**
     * Indicates current lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     * @return index lock mode
     */
    public IndexLockMode getLockMode() {
        return lockMode;
    }

    /**
     * Indicates current lock mode:
     * - Unlock - all index definition changes acceptable
     * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
     * - LockedError - all index definition changes will raise exception
     * @param lockMode Sets the value
     */
    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Indicates index type.
     * @return index type
     */
    public IndexType getType() {
        return type;
    }

    /**
     * Indicates index type.
     * @param type Sets the value
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
     * @return index entries count
     */
    public int getEntriesCount() {
        return entriesCount;
    }

    /**
     * Total number of entries in this index.
     * @param entriesCount sets the value
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
     * @return true if test index
     */
    public boolean isTestIndex() {
        return isTestIndex;
    }

    /**
     * Indicates if this is a test index (works on a limited data set - for testing purposes only)
     * @param testIndex Sets the value
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
