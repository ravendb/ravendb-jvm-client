package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

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
}
