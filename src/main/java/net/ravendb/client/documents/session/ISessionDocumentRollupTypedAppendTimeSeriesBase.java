package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;
import net.ravendb.client.DocumentationUrls;

public interface ISessionDocumentRollupTypedAppendTimeSeriesBase<T> {
    /**
     * Appends rollup values to the time series at the provided timestamp.
     *
     * <p>For more information on the Time Series Append operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#AppendOperation}.</p>
     * <p>For details about named time series values, refer to:
     * {@link DocumentationUrls.Session.TimeSeries#NamedValues}.</p>
     * @param entry The time series arguments to append (timestamp and rollup values).
     * <code>TimeSeriesRollupEntry</code> is an extension of <code>TimeSeriesEntry</code>}
     * which contains the aggregated values of rollup time series [Min, Max, First, Last, Sum, Count, Average].
     */
    void append(TypedTimeSeriesRollupEntry<T> entry);
}
