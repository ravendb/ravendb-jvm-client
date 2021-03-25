package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class IncludeBuilder extends IncludeBuilderBase implements IIncludeBuilder {

    public IncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public IncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }


}
