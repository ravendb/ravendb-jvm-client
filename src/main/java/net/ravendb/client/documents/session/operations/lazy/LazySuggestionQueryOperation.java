package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class LazySuggestionQueryOperation implements ILazyOperation {

    private Object result;
    private QueryResult queryResult;
    private boolean requiresRetry;

    private final InMemoryDocumentSessionOperations _session;
    private final IndexQuery _indexQuery;
    private final Consumer<QueryResult> _invokeAfterQueryExecuted;
    private final Function<QueryResult, Map<String, SuggestionResult>> _processResults;

    public LazySuggestionQueryOperation(InMemoryDocumentSessionOperations session, IndexQuery indexQuery, Consumer<QueryResult> invokeAfterQueryExecuted,
                                        Function<QueryResult, Map<String, SuggestionResult>> processResults) {
        _session = session;
        _indexQuery = indexQuery;
        _invokeAfterQueryExecuted = invokeAfterQueryExecuted;
        _processResults = processResults;
    }

    @Override
    public GetRequest createRequest() {
        GetRequest request = new GetRequest();
        request.setCanCacheAggressively(!_indexQuery.isDisableCaching() && !_indexQuery.isWaitForNonStaleResults());
        request.setUrl("/queries");
        request.setMethod("POST");
        request.setQuery("?queryHash=" + _indexQuery.getQueryHash(_session.getConventions()));
        request.setContent(new IndexQueryContent(_session.getConventions(), _indexQuery));

        return request;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public QueryResult getQueryResult() {
        return queryResult;
    }

    @Override
    public boolean isRequiresRetry() {
        return requiresRetry;
    }

    @Override
    public void handleResponse(GetResponse response) {
        if (response.isForceRetry()) {
            result = null;
            requiresRetry = true;
            return;
        }

        try {
            QueryResult queryResult = JsonExtensions.getDefaultMapper().readValue(response.getResult(), QueryResult.class);

            handleResponse(queryResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleResponse(QueryResult queryResult) {
        result = _processResults.apply(queryResult);
        this.queryResult = queryResult;
    }
}
