package net.ravendb.client.documents.queries.suggestions;

public interface ISuggestionOperations<T> {
    ISuggestionOperations<T> withDisplayName(String displayName);

    ISuggestionOperations<T> withOptions(SuggestionOptions options);
}
