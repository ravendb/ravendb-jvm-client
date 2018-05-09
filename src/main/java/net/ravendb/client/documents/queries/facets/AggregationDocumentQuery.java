package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.AbstractDocumentQuery;
import net.ravendb.client.documents.session.DocumentQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.util.function.Consumer;

public class AggregationDocumentQuery<T> extends AggregationQueryBase implements IAggregationDocumentQuery<T> {

    private final AbstractDocumentQuery<T, DocumentQuery<T>> _source;

    public AggregationDocumentQuery(DocumentQuery<T> source) {
        super((InMemoryDocumentSessionOperations) source.getSession());

        _source = source;
    }

    @Override
    public IAggregationDocumentQuery<T> andAggregateBy(Consumer<IFacetBuilder<T>> builder) {
        FacetBuilder<T> f = new FacetBuilder<>();
        builder.accept(f);

        return andAggregateBy(f.getFacet());
    }

    @Override
    public IAggregationDocumentQuery<T> andAggregateBy(FacetBase facet) {
        _source._aggregateBy(facet);
        return this;
    }

    @Override
    protected IndexQuery getIndexQuery() {
        return _source.getIndexQuery();
    }

    @Override
    protected void invokeAfterQueryExecuted(QueryResult result) {
        _source.invokeAfterQueryExecuted(result);
    }
}
