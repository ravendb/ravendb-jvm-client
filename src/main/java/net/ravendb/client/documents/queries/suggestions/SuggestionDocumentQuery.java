package net.ravendb.client.documents.queries.suggestions;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.DocumentQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.util.function.Consumer;

/**
 * {@inheritDoc}
 * @see SuggestionQueryBase
 */
public class SuggestionDocumentQuery<T> extends SuggestionQueryBase implements ISuggestionDocumentQuery<T> {

    private final DocumentQuery<T> _source;

    /**
     * {@inheritDoc}
     */
    public SuggestionDocumentQuery(DocumentQuery<T> source) {
        super((InMemoryDocumentSessionOperations) source.getSession());

        _source = source;
    }

    @Override
    protected IndexQuery getIndexQuery() {
        return getIndexQuery(true);
    }

    @Override
    protected IndexQuery getIndexQuery(boolean updateAfterQueryExecuted) {
        return _source.getIndexQuery();
    }

    @Override
    protected void invokeAfterQueryExecuted(QueryResult result) {
        _source.invokeAfterQueryExecuted(result);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionDocumentQuery<T> andSuggestUsing(SuggestionBase suggestion) {
        _source.suggestUsing(suggestion);
        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionDocumentQuery<T> andSuggestUsing(Consumer<ISuggestionBuilder<T>> builder) {
        SuggestionBuilder<T> f = new SuggestionBuilder<>(_source.getConventions());
        builder.accept(f);

        _source.suggestUsing(f.getSuggestion());
        return this;
    }
}
