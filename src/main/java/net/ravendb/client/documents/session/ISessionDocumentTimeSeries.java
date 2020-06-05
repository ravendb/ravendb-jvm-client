package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.Date;
import java.util.List;

/**
 * Time series synchronous session operations
 */
public interface ISessionDocumentTimeSeries extends ISessionDocumentAppendTimeSeriesBase, ISessionDocumentRemoveTimeSeriesBase {
    /**
     * Return all time series values
     * @return time series values
     */
    List<TimeSeriesEntry> get();

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @return time series values
     */
    List<TimeSeriesEntry> get(Date from, Date to);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @return time series values
     */
    List<TimeSeriesEntry> get(Date from, Date to, int start);

    /**
     * Return the time series values for the provided range
     * @param from range start
     * @param to range end
     * @param start start
     * @param pageSize page size
     * @return time series values
     */
    List<TimeSeriesEntry> get(Date from, Date to, int start, int pageSize);
}
