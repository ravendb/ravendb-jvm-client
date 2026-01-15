package net.ravendb.client.documents.session;

import java.util.Date;
import net.ravendb.client.DocumentationUrls;

public interface ISessionDocumentTypedIncrementTimeSeriesBase<T> {
    /**
     * Increments the values of the incremental time series at the provided timestamp using the specified entry.
     *
     * <p>For more information on the Time Series Increment operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#IncrementOperation}.</p>
     * <p>For details about named time series values, refer to:
     * {@link DocumentationUrls.Session.TimeSeries#NamedValues}.</p>
     *
     * @param timestamp The timestamp at which to apply the increment to the original values of the incremental time series.
     * @param entry The entry representing the values to increment the original values of the incremental time series.
     */
    void increment(Date timestamp, T entry);

    /**
     * {@inheritDoc}
     * {@link #increment(Date, Object)}
     */
    void increment(T entry);
}
