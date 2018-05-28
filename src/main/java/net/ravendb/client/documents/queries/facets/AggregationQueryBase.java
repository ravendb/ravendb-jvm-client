package net.ravendb.client.documents.queries.facets;

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
import net.ravendb.client.documents.session.operations.lazy.LazyAggregationQueryOperation;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AggregationQueryBase {

    private final InMemoryDocumentSessionOperations _session;
    private IndexQuery _query;
    private Stopwatch _duration;

    protected AggregationQueryBase(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public Map<String, FacetResult> execute() {
        QueryCommand command = getCommand();

        _duration = Stopwatch.createStarted();

        _session.incrementRequestCount();
        _session.getRequestExecutor().execute(command);

        return processResults(command.getResult(), _session.getConventions());
    }

    public Lazy<Map<String, FacetResult>> executeLazy() {
        return executeLazy(null);
    }

    @SuppressWarnings("unchecked")
    public Lazy<Map<String, FacetResult>> executeLazy(Consumer<Map<String, FacetResult>> onEval) {
        _query = getIndexQuery();
        return ((DocumentSession)_session).addLazyOperation((Class<Map<String, FacetResult>>)(Class<?>)Map.class,
                new LazyAggregationQueryOperation( _session.getConventions(), _query, result -> invokeAfterQueryExecuted(result), this::processResults), onEval);
    }

    protected abstract IndexQuery getIndexQuery();

    protected abstract void invokeAfterQueryExecuted(QueryResult result);

    private Map<String, FacetResult> processResults(QueryResult queryResult, DocumentConventions conventions) {
        invokeAfterQueryExecuted(queryResult);

        Map<String, FacetResult> results = new HashMap<>();
        for (JsonNode result : queryResult.getResults()) {
            FacetResult facetResult = JsonExtensions.getDefaultMapper().convertValue(result, FacetResult.class);
            results.put(facetResult.getName(), facetResult);
        }

        QueryOperation.ensureIsAcceptable(queryResult, _query.isWaitForNonStaleResults(), _duration, _session);
        return results;
    }

    private QueryCommand getCommand() {
        _query = getIndexQuery();

        return new QueryCommand(_session.getConventions(), _query, false, false);
    }

    @Override
    public String toString() {
        return getIndexQuery().toString();
    }
}
