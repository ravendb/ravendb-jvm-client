package net.ravendb.client.documents.queries.suggestions;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.DocumentQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

public class SuggestionDocumentQuery<T> extends SuggestionQueryBase implements ISuggestionDocumentQuery<T> {

    private final DocumentQuery<T> _source;

    public SuggestionDocumentQuery(DocumentQuery<T> source) {
        super((InMemoryDocumentSessionOperations) source.getSession());

        _source = source;
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
