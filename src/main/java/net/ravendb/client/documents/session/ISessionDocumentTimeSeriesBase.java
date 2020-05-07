package net.ravendb.client.documents.session;

import java.util.Date;

/**
 * Time series advanced in memory session operations
 */
public interface ISessionDocumentTimeSeriesBase {

    /**
     * Append the the values to the times series at the provided time stamp
     * @param timestamp date
     * @param values values
     */
    void append(Date timestamp, double[] values);

    /**
     * Append the the values (and optional tag) to the times series at the provided time stamp
     * @param timestamp date
     * @param values values
     * @param tag optional tag
     */
    void append(Date timestamp, double[] values, String tag);

    /**
     * Append a single value to the times series at the provided time stamp
     * @param timestamp date
     * @param value value
     */
    void append(Date timestamp, double value);

    /**
     * Append a single value (and optional tag) to the times series at the provided time stamp
     * @param timestamp date
     * @param value value
     * @param tag optional tag
     */
    void append(Date timestamp, double value, String tag);

    /**
     * Remove all the values in the time series in the range of from .. to.
     * @param from range start
     * @param to range end
     */
    void remove(Date from, Date to);

    /**
     * Remove the value in the time series in the specified time stamp
     * @param at date to remove
     */
    void remove(Date at);
}
