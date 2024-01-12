package net.ravendb.client.documents.changes;

public interface ICounterChanges<TChanges> {

    /**
     * Subscribe for changes for all counters.
     * @return Changes observable
     */
    IChangesObservable<TChanges> forAllCounters();

    /**
     * Subscribe to changes for all counters with a given name.
     * @param counterName Counter name
     * @return Changes observable
     */
    IChangesObservable<TChanges> forCounter(String counterName);

    /**
     * Subscribe to changes for counter from a given document and with given name.
     * @param documentId Document identifier
     * @param counterName Counter name
     * @return Changes observable
     */
    IChangesObservable<TChanges> forCounterOfDocument(String documentId, String counterName);

    /**
     * Subscribe to changes for all counters from a given document.
     * @param documentId Document identifier
     * @return Changes observable
     */
    IChangesObservable<TChanges> forCountersOfDocument(String documentId);
}
