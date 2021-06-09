package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class TimeSeriesIncludeBuilder extends IncludeBuilderBase implements ITimeSeriesIncludeBuilder {
    public TimeSeriesIncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public ITimeSeriesIncludeBuilder includeTags() {
        includeTimeSeriesTags = true;
        return this;
    }

    @Override
    public ITimeSeriesIncludeBuilder includeDocument() {
        includeTimeSeriesDocument = true;
        return this;
    }
}
