package net.ravendb.client.documents.session.loaders;

import java.util.Date;

public interface IQueryIncludeBuilder extends IGenericIncludeBuilder<IQueryIncludeBuilder> {
    IQueryIncludeBuilder includeCounter(String path, String name);

    IQueryIncludeBuilder includeCounters(String path, String[] names);

    IQueryIncludeBuilder includeAllCounters(String path);

    IQueryIncludeBuilder includeTimeSeries(String path, String name);

    IQueryIncludeBuilder includeTimeSeries(String path, String name, Date from, Date to);
}
