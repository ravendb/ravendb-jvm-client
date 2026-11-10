package net.ravendb.client.documents.operations.revisions;

import net.ravendb.client.documents.operations.IOperationProgress;
import net.ravendb.client.documents.operations.IOperationResult;

import java.util.HashMap;
import java.util.Map;

public abstract class OperationResult implements IOperationResult, IOperationProgress {

    private int scannedRevisions;
    private int scannedDocuments;
    private Map<String, String> warnings = new HashMap<>();
    private String message;
    private Map<String, Long> lastProcessedEtags = new HashMap<>();
    private Map<String, Long> etagBarriersUsed = new HashMap<>();
    private Map<String, String> nodeTags = new HashMap<>();

    public int getScannedRevisions() {
        return scannedRevisions;
    }

    public void setScannedRevisions(int scannedRevisions) {
        this.scannedRevisions = scannedRevisions;
    }

    public int getScannedDocuments() {
        return scannedDocuments;
    }

    public void setScannedDocuments(int scannedDocuments) {
        this.scannedDocuments = scannedDocuments;
    }

    public Map<String, String> getWarnings() {
        return warnings;
    }

    public void setWarnings(Map<String, String> warnings) {
        this.warnings = warnings;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * The highest etag among the last-scanned etags across all collections on each database.
     * When multiple collections are processed, this is the maximum of the per-collection
     * last-scanned etags. Pass this value as start from etags to resume the operation.
     * For sharded databases the key is the shard database name (e.g. "Northwind$0");
     * for non-sharded databases it is the database name (e.g. "Northwind").
     * @return last processed etags
     */
    public Map<String, Long> getLastProcessedEtags() {
        return lastProcessedEtags;
    }

    public void setLastProcessedEtags(Map<String, Long> lastProcessedEtags) {
        this.lastProcessedEtags = lastProcessedEtags;
    }

    /**
     * The etag barriers used during this run, keyed by database name.
     * For sharded databases the key is the shard database name (e.g. "Northwind$0");
     * for non-sharded databases it is the database name (e.g. "Northwind").
     * @return etag barriers used
     */
    public Map<String, Long> getEtagBarriersUsed() {
        return etagBarriersUsed;
    }

    public void setEtagBarriersUsed(Map<String, Long> etagBarriersUsed) {
        this.etagBarriersUsed = etagBarriersUsed;
    }

    /**
     * The node tags that processed each database/shard during this run.
     * For sharded databases the key is the shard database name (e.g. "Northwind$0");
     * for non-sharded databases it is the database name (e.g. "Northwind").
     * Pass this value into {@link RevisionsOperationContinuationParameters#setNodeTags(Map)}
     * when resuming the operation to ensure it runs on the same node(s).
     * @return node tags
     */
    public Map<String, String> getNodeTags() {
        return nodeTags;
    }

    public void setNodeTags(Map<String, String> nodeTags) {
        this.nodeTags = nodeTags;
    }

    public boolean canMerge() {
        return false;
    }

    public boolean shouldPersist() {
        return false;
    }
}
