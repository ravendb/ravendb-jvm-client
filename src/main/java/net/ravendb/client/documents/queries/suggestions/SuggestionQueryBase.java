package net.ravendb.client.documents.queries.suggestions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.documents.session.operations.lazy.LazySuggestionQueryOperation;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class SuggestionQueryBase {

    private final InMemoryDocumentSessionOperations _session;
    private IndexQuery _query;
    private Stopwatch _duration;

    protected SuggestionQueryBase(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public Map<String, SuggestionResult> execute() {
        QueryCommand command = getCommand();

        _duration = Stopwatch.createStarted();
        _session.incrementRequestCount();
        _session.getRequestExecutor().execute(command);

        return processResults(command.getResult(), _session.getConventions());
    }

    private Map<String, SuggestionResult> processResults(QueryResult queryResult, DocumentConventions conventions) {
        invokeAfterQueryExecuted(queryResult);

        try {
            Map<String, SuggestionResult> results = new HashMap<>();
            for (JsonNode result : queryResult.getResults()) {
                SuggestionResult suggestionResult = JsonExtensions.getDefaultMapper().treeToValue(result, SuggestionResult.class);
                results.put(suggestionResult.getName(), suggestionResult);
            }

            QueryOperation.ensureIsAcceptable(queryResult, _query.isWaitForNonStaleResults(), _duration, _session);

            return results;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process suggestions results: " + e.getMessage(), e);
        }
    }

    public Lazy<Map<String, SuggestionResult>> executeLazy() {
        return executeLazy(null);
    }

    @SuppressWarnings("unchecked")
    public Lazy<Map<String, SuggestionResult>> executeLazy(Consumer<Map<String, SuggestionResult>> onEval) {
        _query = getIndexQuery();

        return ((DocumentSession)_session).addLazyOperation((Class<Map<String, SuggestionResult>>)(Class<?>)Map.class,
                new LazySuggestionQueryOperation(_session.getConventions(), _query, this::invokeAfterQueryExecuted, this::processResults), onEval);
    }

    protected abstract IndexQuery getIndexQuery();

    protected abstract void invokeAfterQueryExecuted(QueryResult result);

    private QueryCommand getCommand() {
        _query = getIndexQuery();

        return new QueryCommand(_session.getConventions(), _query, false, false);
    }


    @Override
    public String toString() {
        return getIndexQuery().toString();
    }
}
