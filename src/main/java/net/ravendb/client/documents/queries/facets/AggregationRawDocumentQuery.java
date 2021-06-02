package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

public class AggregationRawDocumentQuery<T> extends AggregationQueryBase {
    private final IRawDocumentQuery<T> _source;

    public AggregationRawDocumentQuery(IRawDocumentQuery<T> source, InMemoryDocumentSessionOperations session) {
        super(session);
        _source = source;

        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
    }

    @Override
    protected IndexQuery getIndexQuery() {
        return _source.getIndexQuery();
    }

    @Override
    protected IndexQuery getIndexQuery(boolean updateAfterQueryExecuted) {
        return _source.getIndexQuery();
    }

    @Override
    protected void invokeAfterQueryExecuted(QueryResult result) {
        _source.invokeAfterQueryExecuted(result);
    }
}
