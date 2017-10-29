package net.ravendb.client.documents.session.operations;

import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.time.Duration;
import java.util.List;

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
    //TODO: private static readonly Logger Logger = LoggingSource.Instance.GetLogger<QueryOperation>("Raven.NewClient.Client");


    public QueryOperation(InMemoryDocumentSessionOperations session, String indexName, IndexQuery indexQuery,
                          String[] projectionFields, boolean waitForNonStaleResults, Duration timeout,
                          boolean disableEntitiesTracking, boolean metadataOnly, boolean indexEntriesOnly) {
        _session = session;
        _indexName = indexName;
        _indexQuery = indexQuery;
        _waitForNonStaleResults = waitForNonStaleResults;
        _timeout = timeout;
        _projectionFields = projectionFields;
        //TODO DisableEntitiesTracking = disableEntitiesTracking;
        _metadataOnly = metadataOnly;
        _indexEntriesOnly = indexEntriesOnly;

        assertPageSizeSet();
    }

    public QueryCommand createRequest() {
        _session.incrementRequestCount();

        //TODO: logQuery();

        return new QueryCommand(_session.getConventions(), _indexQuery, _metadataOnly, _indexEntriesOnly);
    }

    /* TODO
        public QueryResult CurrentQueryResults => _currentQueryResults;

        public void SetResult(QueryResult queryResult)
        {
            EnsureIsAcceptableAndSaveResult(queryResult);
        }

        private void AssertPageSizeSet()
        {
            if (_session.Conventions.ThrowIfQueryPageSizeIsNotSet == false)
                return;

            if (_indexQuery.PageSizeSet)
                return;

            throw new InvalidOperationException("Attempt to query without explicitly specifying a page size. You can use .Take() methods to set maximum number of results. By default the page size is set to int.MaxValue and can cause severe performance degradation.");
        }

        private void StartTiming()
        {
            _sp = Stopwatch.StartNew();
        }

        public void LogQuery()
        {
            if (Logger.IsInfoEnabled)
                Logger.Info($"Executing query '{_indexQuery.Query}' on index '{_indexName}' in '{_session.StoreIdentifier}'");
        }

        public IDisposable EnterQueryContext()
        {
            StartTiming();

            if (_waitForNonStaleResults == false)
                return null;

            return _session.DocumentStore.DisableAggressiveCaching();
        }

*/

    public <T> List<T> complete(Class<T> clazz) {
        return null; //TODO
    }

    /* TODO
        public List<T> Complete<T>()
        {
            var queryResult = _currentQueryResults.CreateSnapshot();

            if (DisableEntitiesTracking == false)
                _session.RegisterIncludes(queryResult.Includes);

            var list = new List<T>();
            foreach (BlittableJsonReaderObject document in queryResult.Results)
            {
                var metadata = document.GetMetadata();

                metadata.TryGetId(out var id);

                list.Add(Deserialize<T>(id, document, metadata, _projectionFields, DisableEntitiesTracking, _session));
            }

            if (DisableEntitiesTracking == false)
                _session.RegisterMissingIncludes(queryResult.Results, queryResult.Includes, queryResult.IncludedPaths);

            return list;
        }

        internal static T Deserialize<T>(string id, BlittableJsonReaderObject document, BlittableJsonReaderObject metadata, string[] projectionFields, bool disableEntitiesTracking, InMemoryDocumentSessionOperations session)
        {
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

            var result = (T)session.Conventions.DeserializeEntityFromBlittable(typeof(T), document);

            if (string.IsNullOrEmpty(id) == false)
            {
                // we need to make an additional check, since it is possible that a value was explicitly stated
                // for the identity property, in which case we don't want to override it.
                object value;
                var identityProperty = session.Conventions.GetIdentityProperty(typeof(T));
                if (identityProperty != null && (document.TryGetMember(identityProperty.Name, out value) == false || value == null))
                    session.GenerateEntityIdOnTheClient.TrySetIdentity(result, id);
            }

            return result;
        }

        public bool DisableEntitiesTracking { get; set; }

        public void EnsureIsAcceptableAndSaveResult(QueryResult result)
        {
            if (result == null)
                throw new IndexDoesNotExistException("Could not find index " + _indexName);

            if (_waitForNonStaleResults && result.IsStale)
            {
                _sp.Stop();
                var msg = $"Waited for {_sp.ElapsedMilliseconds:#,#;;0}ms for the query to return non stale result.";

                throw new TimeoutException(msg);
            }

            _currentQueryResults = result;
            _currentQueryResults.EnsureSnapshot();

            if (Logger.IsInfoEnabled)
            {
                var isStale = result.IsStale ? "stale " : "";
                Logger.Info($"Query returned {result.Results.Items.Count()}/{result.TotalResults} {isStale}results");
            }
        }

        public IndexQuery IndexQuery => _indexQuery;

     */
}
