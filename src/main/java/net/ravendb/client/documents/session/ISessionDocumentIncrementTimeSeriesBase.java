package net.ravendb.client.documents.session;

import java.util.Date;
import net.ravendb.client.DocumentationUrls;

public interface ISessionDocumentIncrementTimeSeriesBase {
    /**
     * Increments the values of the incremental time series at the provided timestamp.
     * If no existing values are present, this method behaves as if setting the values.
     *
     * <p>For more information on the Time Series Increment operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#IncrementOperation}.</p>
     *
     * @param timestamp The timestamp at which to apply the increment to the original values of the incremental time series.
     * @param values An iterable collection of values indicating the deltas to increment the original values of the incremental time series.
     */
    void increment(Date timestamp, double[] values);
    /**
     * {@inheritDoc}
     * <p>Note that because there is no provided timestamp, the method will use the current date and time (Instant.now()) for the operation.</p>
     * {@link #increment}
     */
    void increment(double[] values);
    /**
     * {@inheritDoc}
     * {@link #increment}
     * @param value The delta to increment the original value of the incremental time series.
     */
    void increment(Date timestamp, double value);
    /**
     * {@inheritDoc}
     * @see #increment(double[])
     * @param value The delta to increment the original value of the incremental time series.
     */
    void increment(double value);
}
