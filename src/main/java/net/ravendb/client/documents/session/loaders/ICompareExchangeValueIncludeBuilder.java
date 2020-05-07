package net.ravendb.client.documents.session.loaders;

public interface ICompareExchangeValueIncludeBuilder<TBuilder> {
    TBuilder includeCompareExchangeValue(String path);

    //TBD expr TBuilder IncludeCompareExchangeValue(string path);

    //TBD expr TBuilder IncludeCompareExchangeValue(Expression<Func<T, string>> path);

    //TBD expr TBuilder IncludeCompareExchangeValue(Expression<Func<T, IEnumerable<string>>> path);
}
