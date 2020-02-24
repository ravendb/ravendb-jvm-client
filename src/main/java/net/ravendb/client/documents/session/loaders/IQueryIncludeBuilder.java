package net.ravendb.client.documents.session.loaders;

public interface IQueryIncludeBuilder extends IGenericIncludeBuilder<IQueryIncludeBuilder> {
    IQueryIncludeBuilder includeCounter(String path, String name);

    IQueryIncludeBuilder includeCounters(String path, String[] names);

    IQueryIncludeBuilder includeAllCounters(String path);
}
