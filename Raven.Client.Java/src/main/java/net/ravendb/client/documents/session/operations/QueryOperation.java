package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.exceptions.documents.indexes.IndexDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QueryOperation {
    private final InMemoryDocumentSessionOperations _session;
    private final String _indexName;
    private final IndexQuery _indexQuery;
    private final boolean _waitForNonStaleResults;
    private final boolean _metadataOnly;
    private final boolean _indexEntriesOnly;
    private final Duration _timeout;
    private QueryResult _currentQueryResults;
    private final String[] _projectionFields;
    private Stopwatch _sp;
    private boolean _disableEntitiesTracking;
    private static final Log logger = LogFactory.getLog(QueryOperation.class);

    public QueryOperation(InMemoryDocumentSessionOperations session, String indexName, IndexQuery indexQuery,
                          String[] projectionFields, boolean waitForNonStaleResults, Duration timeout,
                          boolean disableEntitiesTracking, boolean metadataOnly, boolean indexEntriesOnly) {
        _session = session;
        _indexName = indexName;
        _indexQuery = indexQuery;
        _waitForNonStaleResults = waitForNonStaleResults;
        _timeout = timeout;
        _projectionFields = projectionFields;
        _disableEntitiesTracking = disableEntitiesTracking;
        _metadataOnly = metadataOnly;
        _indexEntriesOnly = indexEntriesOnly;

        assertPageSizeSet();
    }

    public QueryCommand createRequest() {
        _session.incrementRequestCount();

        logQuery();

        return new QueryCommand(_session.getConventions(), _indexQuery, _metadataOnly, _indexEntriesOnly);
    }

    public QueryResult getCurrentQueryResults() {
        return _currentQueryResults;
    }

    public void setResult(QueryResult queryResult) {
        ensureIsAcceptableAndSaveResult(queryResult);
    }

    private void assertPageSizeSet() {
        if (!_session.getConventions().isThrowIfQueryPageSizeIsNotSet()) {
            return;
        }

        if (_indexQuery.isPageSizeSet()) {
            return;
        }

        throw new IllegalStateException("Attempt to query without explicitly specifying a page size. " +
                "You can use .take() methods to set maximum number of results. By default the page size is set to Integer.MAX_VALUE and can cause severe performance degradation.");
    }

    private void startTiming() {
        _sp = Stopwatch.createStarted();
    }

    public void logQuery() {
        if (logger.isInfoEnabled()) {
            logger.info("Executing query " + _indexQuery.getQuery() + " on index " + _indexName + " in " + _session.storeIdentifier());
        }
    }

    public CleanCloseable enterQueryContext() {
        startTiming();

        if (!_waitForNonStaleResults) {
            return null;
        }

        return _session.getDocumentStore().disableAggressiveCaching();
    }

    public <T> List<T> complete(Class<T> clazz) {
        QueryResult queryResult = _currentQueryResults.createSnapshot();

        if (!_disableEntitiesTracking) {
            _session.registerIncludes(queryResult.getIncludes());
        }

        ArrayList<T> list = new ArrayList<>();
        for (JsonNode document : queryResult.getResults()) {
            ObjectNode metadata = (ObjectNode) document.get(Constants.Documents.Metadata.KEY);
            JsonNode idNode = metadata.get(Constants.Documents.Metadata.ID);

            String id = null;
            if (idNode != null && idNode.isTextual()) {
                id = ((TextNode) idNode).asText();
            }

            list.add(deserialize(clazz, id, (ObjectNode) document, metadata, _projectionFields, _disableEntitiesTracking, _session));
        }

        if (!_disableEntitiesTracking) {
            _session.registerMissingIncludes(queryResult.getResults(), queryResult.getIncludes(), queryResult.getIncludedPaths());
        }

        return list;
    }

    static <T> T deserialize(Class<T> clazz, String id, ObjectNode document, ObjectNode metadata, String[] projectionFields, boolean disableEntitiesTracking, InMemoryDocumentSessionOperations session) {
        /* TODO
          if (metadata.TryGetProjection(out var projection) == false || projection == false)
                return session.TrackEntity<T>(id, document, metadata, disableEntitiesTracking);

            if (projectionFields != null && projectionFields.Length == 1) // we only select a single field
            {
                var type = typeof(T);
                var typeInfo = type.GetTypeInfo();
                if (type == typeof(string) || typeInfo.IsValueType || typeInfo.IsEnum)
                {
                    var projectionField = projectionFields[0];
                    T value;
                    return document.TryGet(projectionField, out value) == false
                        ? default(T)
                        : value;
                }

                if (document.TryGetMember(projectionFields[0], out object inner) == false)
                    return default(T);

                var innerJson = inner as BlittableJsonReaderObject;
                if (innerJson != null)
                    document = innerJson;
            }
         */

        T result = (T) session.getConventions().deserializeEntityFromJson(clazz, document);

        if (StringUtils.isNotEmpty(id)) {
            // we need to make an additional check, since it is possible that a value was explicitly stated
            // for the identity property, in which case we don't want to override it.
            Field identityProperty = session.getConventions().getIdentityProperty(clazz);
            if (identityProperty != null) {
                JsonNode value = document.get(StringUtils.capitalize(identityProperty.getName()));

                if (value == null) {
                    session.getGenerateEntityIdOnTheClient().trySetIdentity(result, id);
                }
            }
        }

        return result;
    }

    public boolean isDisableEntitiesTracking() {
        return _disableEntitiesTracking;
    }

    public void setDisableEntitiesTracking(boolean disableEntitiesTracking) {
        this._disableEntitiesTracking = disableEntitiesTracking;
    }

    public void ensureIsAcceptableAndSaveResult(QueryResult result) {
        if (result == null) {
            throw new IndexDoesNotExistException("Could not find index " + _indexName);
        }

        if (_waitForNonStaleResults && result.isStale()) {
            _sp.stop();

            throw new TimeoutException("Waited for " + _sp.elapsed(TimeUnit.MILLISECONDS) + "ms for the query to return non stale result.");
        }

        _currentQueryResults = result;
        _currentQueryResults.ensureSnapshot();

        if (logger.isInfoEnabled()) {
            String isStale = result.isStale() ? "stale " : "";
            logger.info("Query returned " + result.getResults().size() + "/" + result.getTotalResults() + isStale + "results");
        }
    }


    public IndexQuery getIndexQuery() {
        return _indexQuery;
    }
}
