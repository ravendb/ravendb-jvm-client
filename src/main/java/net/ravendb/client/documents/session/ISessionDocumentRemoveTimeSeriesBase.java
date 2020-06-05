package net.ravendb.client.documents.session;

import java.util.Date;

public interface ISessionDocumentRemoveTimeSeriesBase {

    /**
     * Remove all the values in the time series
     */
    void remove();

    /**
     * Remove the value in the time series in the specified time stamp
     * @param at date to remove
     */
    void remove(Date at);

    /**
     * Remove all the values in the time series in the range of from .. to.
     * @param from range start
     * @param to range end
     */
    void remove(Date from, Date to);


}
