package net.ravendb.client.documents.queries.suggestions;

public interface ISuggestionBuilder<T> {

    ISuggestionOperations<T> byField(String fieldName, String term);

    ISuggestionOperations<T> byField(String fieldName, String[] terms);

    //TBD expr ISuggestionOperations<T> ByField(Expression<Func<T, object>> path, string term);
    //TBD expr ISuggestionOperations<T> ByField(Expression<Func<T, object>> path, string[] terms);

}
