package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.DocumentationUrls;
import java.util.Date;

public interface ISessionDocumentTypedAppendTimeSeriesBase<T> {
    void append(Date timestamp, T entry);
    /**
     * Appends values (and an optional tag) to the time series at the provided timestamp.
     *
     * <p>For more information on the Time Series Append operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#AppendOperation}.</p>
     * <p>For details about named time series values, refer to:
     * {@link DocumentationUrls.Session.TimeSeries#NamedValues}.</p>
     *
     * @param timestamp The timestamp at which the values should be appended to the time series.
     * @param entry An entry representing the typed values to append to the time series (T indicates the value type).
     * @param tag An optional tag to associate with the appended values. Tags can be used to categorize or label the data.
     */

    void append(Date timestamp, T entry, String tag);
    /**
     * Appends values (and an optional tag) to the time series at the provided timestamp.
     *
     * <p>For more information on the Time Series Append operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#AppendOperation}.</p>
     * <p>For details about named time series values, refer to:
     * {@link DocumentationUrls.Session.TimeSeries#NamedValues}.</p>
     *
     * @param entry The time series arguments to append (timestamp, values, optional tag).
     *              <code>TimeSeriesEntry</code> is an object that aggregates the time series inputs, and T indicates the value type.
     */
    void append(TypedTimeSeriesEntry<T> entry);
}
