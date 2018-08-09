package net.ravendb.client.documents.session.loaders;

public interface IQueryIncludeBuilder {

    IQueryIncludeBuilder includeCounter(String name);

    IQueryIncludeBuilder includeCounters(String[] names);

    IQueryIncludeBuilder includeAllCounters();

    IQueryIncludeBuilder includeDocuments(String path);

    IQueryIncludeBuilder includeCounter(String path, String name);

    IQueryIncludeBuilder includeCounters(String path, String[] names);

    IQueryIncludeBuilder includeAllCounters(String path);
}
