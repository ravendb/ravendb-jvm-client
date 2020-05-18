package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

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
        return includeTimeSeries(name, null, null);
    }

    @Override
    public IIncludeBuilder includeTimeSeries(String name, Date from, Date to) {
        _includeTimeSeries("", name, from, to);
        return this;
    }

    @Override
    public IIncludeBuilder includeCompareExchangeValue(String path) {
        _includeCompareExchangeValue(path);
        return this;
    }
}
