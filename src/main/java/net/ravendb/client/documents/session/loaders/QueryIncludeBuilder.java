package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;

import java.util.Date;

public class QueryIncludeBuilder extends IncludeBuilderBase implements IQueryIncludeBuilder {

    public QueryIncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public IQueryIncludeBuilder includeCounter(String path, String name) {
        _includeCounterWithAlias(path, name);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeCounters(String path, String[] names) {
        _includeCounterWithAlias(path, names);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeAllCounters(String path) {
        _includeAllCountersWithAlias(path);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeCounter(String name) {
        _includeCounter("", name);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeCounters(String[] names) {
        _includeCounters("", names);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeAllCounters() {
        _includeAllCounters("");
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String name) {
        return includeTimeSeries(name, (Date) null, null);
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String name, Date from, Date to) {
        _includeTimeSeriesFromTo("", name, from, to);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String path, String name) {
        return includeTimeSeries(path, name, null, null);
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String path, String name, Date from, Date to) {
        _withAlias();
        _includeTimeSeriesFromTo(path, name, from, to);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeCompareExchangeValue(String path) {
        _includeCompareExchangeValue(path);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", name, type, time);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", name, type, count);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time) {
        _includeArrayOfTimeSeriesByRangeTypeAndTime(names, type, time);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count) {
        _includeArrayOfTimeSeriesByRangeTypeAndCount(names, type, count);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", Constants.TimeSeries.ALL, type, time);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", Constants.TimeSeries.ALL, type, count);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeRevisions(Date before) {
        _includeRevisionsBefore(before);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeRevisions(String changeVectorPath) {
        _withAlias();
        _includeRevisionsByChangeVectors(changeVectorPath);
        return this;
    }
}
