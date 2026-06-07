package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum OptimisticConcurrencyMode {
    /**
     * No optimistic concurrency checks are performed.
     * During saveChanges, PUT and DELETE commands are sent without a change vector, so the server does not check
     * for concurrent modifications.
     */
    NONE,

    /**
     * Optimistic concurrency checks are performed for written (PUT) and deleted (DELETE) entities only.
     * Entities that are tracked by the session but were not modified/deleted are NOT checked.
     * <p>
     * During saveChanges, each PUT/DELETE command includes the entity's change vector so the server rejects
     * the save operation if the document was modified by another session since it was first tracked by the session.
     * <p>
     * This mode is incompatible with {@code SessionOptions.noTracking} and {@code TransactionMode.CLUSTER_WIDE}.
     */
    WRITES,

    /**
     * Optimistic concurrency checks are performed for ALL entities tracked by the session - both modified and
     * not modified.
     * <p>
     * During saveChanges, the session sends the change vector of ALL tracked documents to the server.
     * The server rejects the save operation if any of these documents was modified by another session since
     * it was first tracked by the session.
     * <p>
     * This mode is incompatible with {@code SessionOptions.noTracking}, {@code TransactionMode.CLUSTER_WIDE},
     * and sharded databases.
     */
    WRITES_AND_READS
}
