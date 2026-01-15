package net.ravendb.client.documents.queries.suggestions;

public interface ISuggestionOperations<T> {
    /**
     * A custom name for the suggestions result.
     * {@inheritDoc}
     * @param displayName Custom name.
     */
    ISuggestionOperations<T> withDisplayName(String displayName);
    /**
     * Non-default options to use in the operation.
     * {@inheritDoc}
     * @param options Custom name.
     */
    ISuggestionOperations<T> withOptions(SuggestionOptions options);
}
