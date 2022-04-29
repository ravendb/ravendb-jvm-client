package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.primitives.TimeValue;

import java.util.Date;

public class IncludeBuilder extends IncludeBuilderBase implements IIncludeBuilder {

    public IncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public IncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }

    @Override
    public IIncludeBuilder includeCounter(String name) {
        _includeCounter("", name);
        return this;
    }

    @Override
    public IIncludeBuilder includeCounters(String[] names) {
        _includeCounters("", names);
        return this;
    }

    @Override
    public IIncludeBuilder includeAllCounters() {
        _includeAllCounters("");
        return this;
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String name) {
        return includeTimeSeries(name, (Date) null, null);
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String name, Date from, Date to) {
        _includeTimeSeriesFromTo("", name, from, to);
        return this;
    }

    @Override
    public IIncludeBuilder includeCompareExchangeValue(String path) {
        _includeCompareExchangeValue(path);
        return this;
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", name, type, time);
        return this;
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String name, TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", name, type, count);
        return this;
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, TimeValue time) {
        _includeArrayOfTimeSeriesByRangeTypeAndTime(names, type, time);
        return this;
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String[] names, TimeSeriesRangeType type, int count) {
        _includeArrayOfTimeSeriesByRangeTypeAndCount(names, type, count);
        return this;
    }

    @Override
    public IIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, TimeValue time) {
        _includeTimeSeriesByRangeTypeAndTime("", Constants.TimeSeries.ALL, type, time);
        return this;
    }

    @Override
    public IIncludeBuilder includeAllTimeSeries(TimeSeriesRangeType type, int count) {
        _includeTimeSeriesByRangeTypeAndCount("", Constants.TimeSeries.ALL, type, count);
        return this;
    }

    @Override
    public IIncludeBuilder includeRevisions(String changeVectorPaths) {
        _withAlias();
        _includeRevisionsByChangeVectors(changeVectorPaths);
        return this;
    }

    @Override
    public IIncludeBuilder includeRevisions(Date before) {
        _includeRevisionsBefore(before);
        return this;
    }
}
