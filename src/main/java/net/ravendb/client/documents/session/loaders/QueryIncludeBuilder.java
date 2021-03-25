package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.Date;

public class QueryIncludeBuilder extends IncludeBuilderBase implements IQueryIncludeBuilder {

    public QueryIncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public IQueryIncludeBuilder includeDocuments(String path) {
        _includeDocuments(path);
        return this;
    }


}
