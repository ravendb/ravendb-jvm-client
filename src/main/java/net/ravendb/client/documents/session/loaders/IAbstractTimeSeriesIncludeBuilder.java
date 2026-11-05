package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;

import java.util.Date;

public interface IAbstractTimeSeriesIncludeBuilder<TBuilder> {
    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time);

    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count);

    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time);

    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count);

    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time);

    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count);

    /**
     * Includes all time series of the entity.
     * @return builder
     */
    TBuilder includeAllTimeSeries();

    /**
     * Includes all time series of the entity, optionally filtered by a specific time range.
     * @param from The start date and time for the range to include. Can be null for no lower bound.
     * @param to The end date and time for the range to include. Can be null for no upper bound.
     * @return builder
     */
    TBuilder includeAllTimeSeries(Date from, Date to);
}
