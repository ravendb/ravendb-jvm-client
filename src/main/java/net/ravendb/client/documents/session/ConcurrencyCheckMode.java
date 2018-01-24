package net.ravendb.client.documents.session;

public enum ConcurrencyCheckMode {
    /**
     * Automatic optimistic concurrency check depending on UseOptimisticConcurrency setting or provided Change Vector
     */
    AUTO,

    /**
     * Force optimistic concurrency check even if UseOptimisticConcurrency is not set
     */
    FORCED,

    /**
     * Disable optimistic concurrency check even if UseOptimisticConcurrency is set
     */
    DISABLED
}
