package net.ravendb.client.documents.session;

import java.util.Date;
import net.ravendb.client.DocumentationUrls;

public interface ISessionDocumentDeleteTimeSeriesBase {

    /**
     * Deletes a range of entries from a single time series.
     *
     * <p>For more information on the Time Series Delete operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#DeleteOperation}.</p>
     */
    void delete();

    /**
     * Deletes a range of entries from a single time series.
     *
     * <p>For more information on the Time Series Delete operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#DeleteOperation}.</p>
     * @param at The specific date and time at which to delete the time series entry.
     */
    void delete(Date at);

    /**
     * Deletes a range of entries from a single time series.
     *
     * <p>For more information on the Time Series Delete operation, see:
     * {@link DocumentationUrls.Session.TimeSeries#DeleteOperation}.</p>
     *
     * @param from The date and time from which to start deleting time series entries (inclusive).
     *             If not specified, the deletion will start from the earliest possible date and time (LocalDateTime.MIN).
     * @param to The date and time indicating the end of the range for deleting time series entries (inclusive).
     *           If not specified, the deletion will continue until the latest possible date and time (LocalDateTime.MAX).
     */
    void delete(Date from, Date to);


}
