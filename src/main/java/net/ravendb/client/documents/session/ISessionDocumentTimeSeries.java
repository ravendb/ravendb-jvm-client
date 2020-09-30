package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.Date;

/**
 * Time series synchronous session operations
 */
public interface ISessionDocumentTimeSeries extends ISessionDocumentAppendTimeSeriesBase, ISessionDocumentDeleteTimeSeriesBase {
    /**
     * Return all time series values
     * @return time series values
     */
    TimeSeriesEntry[] get();

    /**
     * Return all time series values with paging
     * @param start start
     * @param pageSize page size
     * @return time series values
     */
    TimeSeriesEntry[] get(int start, int pageSize);


    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @return time series values
     */
    TimeSeriesEntry[] get(Date from, Date to);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @return time series values
     */
    TimeSeriesEntry[] get(Date from, Date to, int start);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @param pageSize page size
     * @return time series values
     */
    TimeSeriesEntry[] get(Date from, Date to, int start, int pageSize);
}
