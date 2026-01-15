package net.ravendb.client.documents.session;

import java.time.*;
import java.util.Iterator;

public interface ITimeSeriesStreamingBase<T> {

    /**
     * Creates a stream to fetch time series entries within the specified time range.
     *
     * @param from   The starting timestamp for fetching time series entries (inclusive).
     *               If null, will start from the earliest possible date and time (Instant.MIN).
     * @param to     The ending timestamp for fetching time series entries (inclusive).
     *               If null, will continue until the latest possible date and time (Instant.MAX).
     * @param offset The offset to apply to the timestamps when fetching entries.
     *               If null, considered as zero duration.
     * @return An iterator used to iterate over the time series entries received via the stream.
     */
    Iterator<T> stream(Instant from, Instant to, Duration offset);
}

