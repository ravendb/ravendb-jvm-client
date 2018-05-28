package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class LazySuggestionQueryOperation implements ILazyOperation {

    private Object result;
    private QueryResult queryResult;
    private boolean requiresRetry;

    private final DocumentConventions _conventions;
    private final IndexQuery _indexQuery;
    private final Consumer<QueryResult> _invokeAfterQueryExecuted;
    private final BiFunction<QueryResult, DocumentConventions, Map<String, SuggestionResult>> _processResults;

    public LazySuggestionQueryOperation(DocumentConventions conventions, IndexQuery indexQuery, Consumer<QueryResult> invokeAfterQueryExecuted,
                                        BiFunction<QueryResult, DocumentConventions, Map<String, SuggestionResult>> processResults) {
        _conventions = conventions;
        _indexQuery = indexQuery;
        _invokeAfterQueryExecuted = invokeAfterQueryExecuted;
        _processResults = processResults;
    }

    @Override
    public GetRequest createRequest() {
        GetRequest request = new GetRequest();
        request.setUrl("/queries");
        request.setMethod("POST");
        request.setQuery("?queryHash=" + _indexQuery.getQueryHash());
        request.setContent(new IndexQueryContent(_conventions, _indexQuery));

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
        if (_invokeAfterQueryExecuted != null) {
            _invokeAfterQueryExecuted.accept(queryResult);
        }

        result = _processResults.apply(queryResult, _conventions);
        this.queryResult = queryResult;
    }
}
