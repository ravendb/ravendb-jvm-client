package net.ravendb.client.documents.operations.revisions;

import java.util.Map;

/**
 * Contains the state needed to resume an interrupted revisions operation (enforce, adopt, or revert)
 * from where a previous run left off.
 * <p>
 * Populate this from the {@link OperationResult#getLastProcessedEtags()}, {@link OperationResult#getEtagBarriersUsed()}
 * and {@link OperationResult#getNodeTags()} of a completed run.
 * <p>
 * <b>Important:</b> Because etags are node-local, a resumed operation <b>must</b> run on the same
 * database node as the original run. Use {@code store.maintenance().forNode(nodeTag)} (or
 * {@code store.operations().forNode(nodeTag)}) to pin the request. When node tags are
 * populated, the server validates that the operation is executing on the correct node and throws on mismatch.
 */
public class RevisionsOperationContinuationParameters {

    private Map<String, Long> startFromEtags;
    private Map<String, Long> etagBarriers;
    private Map<String, String> nodeTags;

    /**
     * Starting etags for resuming an interrupted operation, keyed by database name.
     * For non-sharded databases the key is the database name (e.g. "Northwind").
     * For sharded databases each shard uses its own name (e.g. "Northwind$0").
     * Populate from {@link OperationResult#getLastProcessedEtags()} of the previous run.
     * @return start from etags
     */
    public Map<String, Long> getStartFromEtags() {
        return startFromEtags;
    }

    /**
     * @param startFromEtags Starting etags for resuming an interrupted operation, keyed by database name.
     */
    public void setStartFromEtags(Map<String, Long> startFromEtags) {
        this.startFromEtags = startFromEtags;
    }

    /**
     * Etag barriers from the previous run, keyed by database name.
     * Must match the {@link OperationResult#getEtagBarriersUsed()} from the previous run so that documents
     * modified between the original barrier and the start from etags are not incorrectly skipped.
     * @return etag barriers
     */
    public Map<String, Long> getEtagBarriers() {
        return etagBarriers;
    }

    /**
     * @param etagBarriers Etag barriers from the previous run, keyed by database name.
     */
    public void setEtagBarriers(Map<String, Long> etagBarriers) {
        this.etagBarriers = etagBarriers;
    }

    /**
     * Node tags from the previous run, keyed by database/shard name.
     * Populate from {@link OperationResult#getNodeTags()} of the previous run.
     * When set, the server validates that the resumed operation is running on the same node
     * that produced the etags and throws if there is a mismatch.
     * @return node tags
     */
    public Map<String, String> getNodeTags() {
        return nodeTags;
    }

    /**
     * @param nodeTags Node tags from the previous run, keyed by database/shard name.
     */
    public void setNodeTags(Map<String, String> nodeTags) {
        this.nodeTags = nodeTags;
    }
}
