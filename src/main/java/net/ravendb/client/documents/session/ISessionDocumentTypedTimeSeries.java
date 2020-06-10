package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;

import java.util.Date;
import java.util.List;

public interface ISessionDocumentTypedTimeSeries<TValues> extends
        ISessionDocumentTypedAppendTimeSeriesBase<TValues>,
        ISessionDocumentRemoveTimeSeriesBase {

    /**
     * Return the time series values for the provided range
     * @return time series values
     */
    TypedTimeSeriesEntry<TValues>[] get();

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @return time series values
     */
    TypedTimeSeriesEntry<TValues>[] get(Date from, Date to);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @return time series values
     */
    TypedTimeSeriesEntry<TValues>[] get(Date from, Date to, int start);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @param pageSize page size
     * @return time series values
     */
    TypedTimeSeriesEntry<TValues>[] get(Date from, Date to, int start, int pageSize);
}
