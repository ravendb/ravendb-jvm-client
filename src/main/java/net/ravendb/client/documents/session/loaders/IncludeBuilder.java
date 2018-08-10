package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class IncludeBuilder extends IncludeBuilderBase implements IIncludeBuilder {

    public IncludeBuilder(DocumentConventions conventions) {
        super(conventions);
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
    public IIncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }

}
