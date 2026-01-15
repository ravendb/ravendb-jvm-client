package net.ravendb.client.documents.queries.suggestions;

public interface ISuggestionBuilder<T> {
    /**
     * {@inheritDoc}
     * @param fieldName Field on which perform term-search.
     * @param term The term for which to get suggested similar terms.
     */
    ISuggestionOperations<T> byField(String fieldName, String term);
    /**
     * {@inheritDoc}
     * @param fieldName Field on which perform term-search.
     * @param terms List of terms for which to get suggested similar terms.
     */
    ISuggestionOperations<T> byField(String fieldName, String[] terms);

    //TODO: expr ISuggestionOperations<T> ByField(Expression<Func<T, object>> path, string term);
    //TODO: expr ISuggestionOperations<T> ByField(Expression<Func<T, object>> path, string[] terms);
}
