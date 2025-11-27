package net.ravendb.client.documents.session;

import java.util.Date;
import net.ravendb.client.DocumentationUrls;

public interface ISessionDocumentAppendTimeSeriesBase {
    /**
     * Appends values (and an optional tag) to the time series at the provided timestamp.
     * @param timestamp date
     * @param values values
     */
    void append(Date timestamp, double[] values);

    /**
     * Appends values (and an optional tag) to the time series at the provided timestamp.
     *
     * <p>For more information on the Time Series Append operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#AppendOperation}.</p>
     *
     * @param timestamp The timestamp at which the values should be appended to the time series.
     * @param values An iterable collection of values to append to the time series.
     * @param tag An optional tag to associate with the appended values. Tags can be used to categorize or label the data.
     */

    void append(Date timestamp, double[] values, String tag);

    /**
     * {@inheritDoc}
     * @see #append(Date, double[], String)
     * @param value The value to append to the time series.
     */
    void append(Date timestamp, double value);

    /**
     * Append a single value (and optional tag) to the times series at the provided time stamp
     * @param timestamp date
     * @param value value
     * @param tag optional tag
     */
    void append(Date timestamp, double value, String tag);
}
