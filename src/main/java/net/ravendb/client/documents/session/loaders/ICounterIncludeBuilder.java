package net.ravendb.client.documents.session.loaders;

public interface ICounterIncludeBuilder<TBuilder> {

    TBuilder includeCounter(String name);

    TBuilder includeCounters(String[] names);

    TBuilder includeAllCounters();
}