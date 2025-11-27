package net.ravendb.client.documents.session.loaders;

/**
 * The server is instructed to include various types of related items when retrieving documents.
 * The items are added to the session unit of work, and subsequent requests to load them are served directly from the session cache,
 * without requiring any additional queries to the server.
 * This interface combines functionalities for including documents, counters, time series, compare exchange values, and revisions.
 * @param <TBuilder> The type of the builder being used.
 */
public interface IGenericIncludeBuilder<TBuilder> extends IDocumentIncludeBuilder<TBuilder>,
        ICounterIncludeBuilder<TBuilder>,
        IGenericTimeSeriesIncludeBuilder<TBuilder>,
        ICompareExchangeValueIncludeBuilder<TBuilder>,
        IGenericRevisionIncludeBuilder<TBuilder> {
}
