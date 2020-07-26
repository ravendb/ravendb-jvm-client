package net.ravendb.client.documents.session;

import java.util.Date;

public interface ISessionDocumentDeleteTimeSeriesBase {

    /**
     * Delete all the values in the time series
     */
    void delete();

    /**
     * Delete the value in the time series in the specified time stamp
     * @param at date to remove
     */
    void delete(Date at);

    /**
     * Delete all the values in the time series in the range of from .. to.
     * @param from range start
     * @param to range end
     */
    void delete(Date from, Date to);


}
