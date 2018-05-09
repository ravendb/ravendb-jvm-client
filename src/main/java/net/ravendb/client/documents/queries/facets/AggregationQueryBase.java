package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.HashMap;
import java.util.Map;

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

    //TBD public Lazy<Dictionary<string, FacetResult>> ExecuteLazy(Action<Dictionary<string, FacetResult>> onEval = null)

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
