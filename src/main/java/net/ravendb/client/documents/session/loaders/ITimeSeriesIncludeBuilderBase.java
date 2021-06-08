package net.ravendb.client.documents.session.loaders;

public interface ITimeSeriesIncludeBuilderBase {
    ITimeSeriesIncludeBuilderBase includeTags();

    ITimeSeriesIncludeBuilderBase includeDocument();
}
