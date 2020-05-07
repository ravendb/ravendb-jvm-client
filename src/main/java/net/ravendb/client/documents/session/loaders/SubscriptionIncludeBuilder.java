package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

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
}
