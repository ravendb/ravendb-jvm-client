package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class TimeSeriesIncludeBuilder extends IncludeBuilderBase implements ITimeSeriesIncludeBuilderBase {
    public TimeSeriesIncludeBuilder(DocumentConventions conventions) {
        super(conventions);
    }

    @Override
    public ITimeSeriesIncludeBuilderBase includeTags() {
        includeTimeSeriesTags = true;
        return this;
    }

    @Override
    public ITimeSeriesIncludeBuilderBase includeDocument() {
        includeTimeSeriesDocument = true;
        return this;
    }
}
