package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.DocumentationUrls;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * The server is instructed to include time series data when retrieving documents.
 * The time series results are added to the session unit of work, and subsequent requests to load them are served directly from the session cache,
 * without requiring any additional requests to the server.
 *
 * <p>{@link DocumentationUrls.Session.TimeSeries#IncludeWithQuery}</p>
 */
public interface IAbstractTimeSeriesIncludeBuilder<TBuilder> {
    /**
     * {@inheritDoc}
     *
     * @param name The name of the time series to include.
     * @param type Indicates how to retrieve the time series entries.
     *             When set to 'Last', retrieves entries from the end of the time series within the specified time range.
     *             <p>Note that {@link TimeSeriesRangeType} cannot be 'None' when time is specified.</p>
     * @param time The time range to consider when retrieving time series entries.
     */
    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time);
    /**
     * {@inheritDoc}
     *
     * @param name The name of the time series to include.
     * @param type Indicates how to retrieve the time series entries.
     *             When set to 'Last', retrieves the last X entries, where X is determined by the 'count' parameter.
     *             <p>Note that {@link TimeSeriesRangeType} cannot be 'None' when count is specified.</p>
     * @param count The maximum number of entries to take when retrieving time series entries.
     */
    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count);
    /**
     * {@inheritDoc}
     * @see #includeTimeSeries(String, TimeSeriesRangeType, TimeValue)
     * @param names The names of the time series to include.
     */
    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time);
    /**
     * {@inheritDoc}
     * @see #includeTimeSeries(String, TimeSeriesRangeType, int)
     * @param names The names of the time series to include.
     */
    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count);

    /**
     * {@inheritDoc}
     * @see #includeTimeSeries(String, TimeSeriesRangeType, TimeValue)
     */
    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time);

    /**
     * {@inheritDoc}
     * @see #includeTimeSeries(String, TimeSeriesRangeType, int)
     */
    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count);

    /**
     * Includes all time series of the entity, optionally filtered by a specific time range.
     *
     * @param from The start date and time for the range to include. Can be {@code null} for no lower bound.
     * @param to The end date and time for the range to include. Can be {@code null} for no upper bound.
     * @return An instance of the builder for method chaining.
     */
    TBuilder includeAllTimeSeries(@Nullable Date from, @Nullable Date to);
}
