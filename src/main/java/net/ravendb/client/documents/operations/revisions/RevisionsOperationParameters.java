package net.ravendb.client.documents.operations.revisions;

/**
 * Base class for revisions operation parameters, providing the common resume-from properties
 * shared by {@link EnforceRevisionsConfigurationOperation.Parameters} and
 * {@link AdoptOrphanedRevisionsOperation.Parameters}.
 */
public class RevisionsOperationParameters {

    private String[] collections;
    private RevisionsOperationContinuationParameters continuationParameters;

    /**
     * Gets the collections the operation applies to.
     * If null, the operation applies to all collections.
     * @return collections
     */
    public String[] getCollections() {
        return collections;
    }

    /**
     * Sets the collections the operation applies to.
     * If null, the operation applies to all collections.
     * @param collections collections
     */
    public void setCollections(String[] collections) {
        this.collections = collections;
    }

    /**
     * Optional continuation state from a previous run.
     * Populate from the completed operation's {@link OperationResult#getLastProcessedEtags()},
     * {@link OperationResult#getEtagBarriersUsed()} and {@link OperationResult#getNodeTags()} to resume where it left off.
     * <p>
     * <b>Important:</b> When resuming, the operation must target the same database node as the
     * original run because etags are node-local. Use {@code store.maintenance().forNode(nodeTag)}
     * (or {@code store.operations().forNode(nodeTag)}) to pin the request.
     * @return continuation parameters
     */
    public RevisionsOperationContinuationParameters getContinuationParameters() {
        return continuationParameters;
    }

    /**
     * @param continuationParameters Optional continuation state from a previous run.
     * @see #getContinuationParameters()
     */
    public void setContinuationParameters(RevisionsOperationContinuationParameters continuationParameters) {
        this.continuationParameters = continuationParameters;
    }

    String nodeTagFor(String databaseName) {
        if (continuationParameters != null && continuationParameters.getNodeTags() != null) {
            return continuationParameters.getNodeTags().get(databaseName);
        }

        return null;
    }
}
