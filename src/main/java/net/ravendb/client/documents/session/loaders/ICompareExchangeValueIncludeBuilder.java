package net.ravendb.client.documents.session.loaders;

/**
 * The server is instructed to include Compare Exchange values when retrieving documents.<br/>
 * Compare Exchange items can be included both when loading entities and during query execution.
 * The session automatically tracks the included Compare Exchange items, allowing their values
 * to be accessed without making additional calls to the server.
 */
public interface ICompareExchangeValueIncludeBuilder<TBuilder> {
    /**
     * Include a single Compare Exchange value by specifying its key.
     * The key should correspond to the Compare Exchange item you wish to include.
     *
     * @param path The key of the Compare Exchange value to include.
     */
    TBuilder includeCompareExchangeValue(String path);

    //TBD expr TBuilder IncludeCompareExchangeValue(Expression<Func<T, string>> path);

    //TBD expr TBuilder IncludeCompareExchangeValue(Expression<Func<T, IEnumerable<string>>> path);
}
