package net.ravendb.client.documents.session.loaders;

public interface IIncludeBuilder {

    IIncludeBuilder includeCounter(String name);

    IIncludeBuilder includeCounters(String[] names);

    IIncludeBuilder includeAllCounters();

    IIncludeBuilder includeDocuments(String path);

    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, IEnumerable<string>>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, IEnumerable<string>>> path);
}