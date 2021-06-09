package net.ravendb.client.documents.session.loaders;

public interface ITimeSeriesIncludeBuilder {
    ITimeSeriesIncludeBuilder includeTags();

    ITimeSeriesIncludeBuilder includeDocument();
}
