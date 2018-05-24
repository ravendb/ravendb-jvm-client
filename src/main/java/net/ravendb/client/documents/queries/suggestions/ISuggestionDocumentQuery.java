package net.ravendb.client.documents.queries.suggestions;

import net.ravendb.client.documents.Lazy;

import java.util.Map;
import java.util.function.Consumer;

public interface ISuggestionDocumentQuery<T> {

    Map<String, SuggestionResult> execute();

    Lazy<Map<String, SuggestionResult>> executeLazy();

    Lazy<Map<String, SuggestionResult>> executeLazy(Consumer<Map<String, SuggestionResult>> onEval);
}
