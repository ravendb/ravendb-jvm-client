package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

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
        return includeTimeSeries(name, null, null);
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String name, Date from, Date to) {
        _includeTimeSeries("", name, from, to);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String path, String name) {
        return includeTimeSeries(path, name, null, null);
    }

    @Override
    public IQueryIncludeBuilder includeTimeSeries(String path, String name, Date from, Date to) {
        _withAlias();
        _includeTimeSeries(path, name, from, to);
        return this;
    }

    @Override
    public IQueryIncludeBuilder includeCompareExchangeValue(String path) {
        _includeCompareExchangeValue(path);
        return this;
    }
}
