package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;

public interface IAbstractTimeSeriesIncludeBuilder<TBuilder> {
    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time);

    TBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count);

    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time);

    TBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count);

    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time);

    TBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count);
}
