package net.ravendb.client.documents.session;

/**
 * Counters advanced synchronous session operations
 */
public interface ISessionDocumentCountersBase {

    /**
     * Increments the value of a counter
     * @param counter the counter name
     */
    void increment(String counter);

    /**
     * Increments by delta the value of a counter
     * @param counter the counter name
     * @param delta increment delta
     */
    void increment(String counter, long delta);

    /**
     * Marks the specified document's counter for deletion. The counter will be deleted when
     * saveChanges is called.
     * @param counter The counter name
     */
    void delete(String counter);
}
