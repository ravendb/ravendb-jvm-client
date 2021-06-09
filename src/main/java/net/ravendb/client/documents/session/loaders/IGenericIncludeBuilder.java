package net.ravendb.client.documents.session.loaders;

public interface IGenericIncludeBuilder<TBuilder> extends IDocumentIncludeBuilder<TBuilder>,
        ICounterIncludeBuilder<TBuilder>, IGenericTimeSeriesIncludeBuilder<TBuilder>, ICompareExchangeValueIncludeBuilder<TBuilder> {
}
