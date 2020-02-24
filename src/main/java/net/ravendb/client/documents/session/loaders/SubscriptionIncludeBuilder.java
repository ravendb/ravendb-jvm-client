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
}
