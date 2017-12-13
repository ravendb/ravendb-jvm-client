package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.tokens.FieldsToFetchToken;
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
    private final boolean _metadataOnly;
    private final boolean _indexEntriesOnly;
    private QueryResult _currentQueryResults;
    private final FieldsToFetchToken _fieldsToFetch;
    private Stopwatch _sp;
    private boolean _disableEntitiesTracking;
    private static final Log logger = LogFactory.getLog(QueryOperation.class);

    public QueryOperation(InMemoryDocumentSessionOperations session, String indexName, IndexQuery indexQuery,
                          FieldsToFetchToken fieldsToFetch, boolean disableEntitiesTracking, boolean metadataOnly, boolean indexEntriesOnly) {
        _session = session;
        _indexName = indexName;
        _indexQuery = indexQuery;
        _fieldsToFetch = fieldsToFetch;
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

        if (!_indexQuery.isWaitForNonStaleResults()) {
            return null;
        }

        return _session.getDocumentStore().disableAggressiveCaching(_session.getDatabaseName());
    }

    public <T> List<T> complete(Class<T> clazz) {
        QueryResult queryResult = _currentQueryResults.createSnapshot();

        if (!_disableEntitiesTracking) {
            _session.registerIncludes(queryResult.getIncludes());
        }

        ArrayList<T> list = new ArrayList<>();

        try {
            for (JsonNode document : queryResult.getResults()) {
                ObjectNode metadata = (ObjectNode) document.get(Constants.Documents.Metadata.KEY);
                JsonNode idNode = metadata.get(Constants.Documents.Metadata.ID);

                String id = null;
                if (idNode != null && idNode.isTextual()) {
                    id = ((TextNode) idNode).asText();
                }

                list.add(deserialize(clazz, id, (ObjectNode) document, metadata, _fieldsToFetch, _disableEntitiesTracking, _session));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to read json: " + e.getMessage(), e);
        }

        if (!_disableEntitiesTracking) {
            _session.registerMissingIncludes(queryResult.getResults(), queryResult.getIncludes(), queryResult.getIncludedPaths());
        }

        return list;
    }

    static <T> T deserialize(Class<T> clazz, String id, ObjectNode document, ObjectNode metadata, FieldsToFetchToken fieldsToFetchToken, boolean disableEntitiesTracking, InMemoryDocumentSessionOperations session) throws JsonProcessingException {
        /* TODO
          if (metadata.TryGetProjection(out var projection) == false || projection == false)
                return session.TrackEntity<T>(id, document, metadata, disableEntitiesTracking);

             if (fieldsToFetch?.Projections != null && fieldsToFetch.Projections.Length == 1) // we only select a single field
            {
                var type = typeof(T);
                var typeInfo = type.GetTypeInfo();
                if (type == typeof(string) || typeInfo.IsValueType || typeInfo.IsEnum)
                {
                    var projectionField = fieldsToFetch.Projections[0];
                    T value;
                    return document.TryGet(projectionField, out value) == false
                        ? default(T)
                        : value;
                }

                if (document.TryGetMember(fieldsToFetch.Projections[0], out object inner) == false)
                    return default(T);

                if (fieldsToFetch.FieldsToFetch != null && fieldsToFetch.FieldsToFetch[0] == fieldsToFetch.Projections[0])
                {
                    if (inner is BlittableJsonReaderObject innerJson) //extraction from original type
                        document = innerJson;
                }
            }
         */

        T result = session.getConventions().getEntityMapper().treeToValue(document, clazz);

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

        ensureIsAcceptable(result, _indexQuery.isWaitForNonStaleResults(), _sp, _session);

        _currentQueryResults = result;
        //_currentQueryResults.ensureSnapshot();

        if (logger.isInfoEnabled()) {
            String isStale = result.isStale() ? "stale " : " ";
            logger.info("Query returned " + result.getResults().size() + "/" + result.getTotalResults() + isStale + "results");
        }
    }

    public static void ensureIsAcceptable(QueryResult result, boolean waitForNonStaleResults, Stopwatch duration, InMemoryDocumentSessionOperations session) {
        if (waitForNonStaleResults && result.isStale()) {
            duration.stop();

            String msg = "Waited for " + duration.toString() + " for the query to return non stale result.";
            throw new TimeoutException(msg);

        }
    }


    public IndexQuery getIndexQuery() {
        return _indexQuery;
    }
}
