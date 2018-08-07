package net.ravendb.client.documents.session.loaders;

public interface IIncludeBuilder<TSelf> {

    TSelf includeCounter(String name);

    TSelf includeCounters(String[] names);

    TSelf includeAllCounters();

    TSelf includeDocuments(String path);

    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, IEnumerable<string>>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, IEnumerable<string>>> path);
}