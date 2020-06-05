package net.ravendb.client.documents.session;

import java.util.Date;

public interface ISessionDocumentAppendTimeSeriesBase {
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
}
