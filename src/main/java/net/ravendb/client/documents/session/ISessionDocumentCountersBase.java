package net.ravendb.client.documents.session;

/**
 * Counters advanced synchronous session operations
 */
public interface ISessionDocumentCountersBase {

    /**
     * Increments the counter value by the provided delta, or by 1 if delta is not provided.
     * @param counter The counter to increment
     */
    void increment(String counter);

    /**
     * Increments the counter value by the provided delta, or by 1 if delta is not provided.
     * @param counter The counter to increment
     * @param delta The value to increment by
     */
    void increment(String counter, long delta);

    /**
     * Marks the specified document's counter for deletion. The counter will be deleted when <code>saveChanges</code>
     * is called.
     * @param counter The counter to delete
     */
    void delete(String counter);
}
