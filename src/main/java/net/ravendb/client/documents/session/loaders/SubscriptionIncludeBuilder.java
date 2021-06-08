package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;

public class SubscriptionIncludeBuilder extends IncludeBuilderBase implements ISubscriptionIncludeBuilder {
    public SubscriptionIncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public ISubscriptionIncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeCounter(String name) {
        _includeCounter("", name);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeCounters(String[] names) {
        _includeCounters("", names);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeAllCounters() {
        _includeAllCounters("");
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", name, type, time);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", name, type, count);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time) {
        _includeArrayOfTimeSeriesByRangeTypeAndTime(names, type, time);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count) {
        _includeArrayOfTimeSeriesByRangeTypeAndCount(names, type, count);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", Constants.TimeSeries.ALL, type, time);
        return this;
    }

    @Override
    public ISubscriptionIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", Constants.TimeSeries.ALL, type, count);
        return this;
    }
}
