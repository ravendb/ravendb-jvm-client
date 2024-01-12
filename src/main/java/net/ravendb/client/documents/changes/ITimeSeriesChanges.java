package net.ravendb.client.documents.changes;

public interface ITimeSeriesChanges<TChanges> {
    /**
     * Subscribe to changes for all timeseries.
     * @return Changes observable
     */
    IChangesObservable<TChanges> forAllTimeSeries();

    /**
     * Subscribe to changes for all timeseries with a given name.
     * @param timeSeriesName Time series name
     * @return Changes observable
     */
    IChangesObservable<TChanges> forTimeSeries(String timeSeriesName);

    /**
     * Subscribe to changes for timeseries from a given document and with given name.
     * @param documentId Document identifier
     * @param timeSeriesName Time series name
     * @return Changes observable
     */
    IChangesObservable<TChanges> forTimeSeriesOfDocument(String documentId, String timeSeriesName);

    /**
     * Subscribe to changes for timeseries from a given document and with given name.
     * @param documentId Document identifier
     * @return Changes observable
     */
    IChangesObservable<TChanges> forTimeSeriesOfDocument(String documentId);
}
