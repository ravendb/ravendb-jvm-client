package net.ravendb.client.documents.session;

import com.google.common.base.Defaults;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.commands.HeadDocumentCommand;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.linq.IDocumentQueryGenerator;
import net.ravendb.client.documents.session.loaders.ILoaderWithInclude;
import net.ravendb.client.documents.session.loaders.MultiLoaderWithInclude;
import net.ravendb.client.documents.session.operations.BatchOperation;
import net.ravendb.client.documents.session.operations.GetRevisionOperation;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.http.RequestExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DocumentSession extends InMemoryDocumentSessionOperations implements IAdvancedSessionOperations, IDocumentSessionImpl, IDocumentQueryGenerator {

    /**
     * Get the accessor for advanced operations
     *
     * Note: Those operations are rarely needed, and have been moved to a separate
     * property to avoid cluttering the API
     */
    @Override
    public IAdvancedSessionOperations advanced() {
        return this;
    }

    /**
     * Access the lazy operations
     */
    /* TBD
    @Override
    public ILazySessionOperations lazily() {
        return this; //TBD  return LazySessionOperations here - see old client!
    }*/

    /**
     * Access the eager operations
     */
    /* TBD
    @Override
    public IEagerSessionOperations eagerly() {
        return this;
    }
    */

    //TBD public IAttachmentsSessionOperations Attachments { get; }
    //TBD public IRevisionsSessionOperations Revisions { get; }

    /**
     * Initializes new DocumentSession
     */
    public DocumentSession(String dbName, DocumentStore documentStore, UUID id, RequestExecutor requestExecutor) {
        super(dbName, documentStore, requestExecutor, id);

        //TBD Attachments = new DocumentSessionAttachments(this);
        //TBD Revisions = new DocumentSessionRevisions(this);
    }

    /**
     * Saves all the changes to the Raven server.
     */
    @Override
    public void saveChanges() {
        BatchOperation saveChangeOperation = new BatchOperation(this);

        try (BatchCommand command = saveChangeOperation.createRequest()) {
            if (command == null) {
                return;
            }

            _requestExecutor.execute(command, sessionInfo);
            saveChangeOperation.setResult(command.getResult());
        }
    }

    /**
     * Check if document exists without loading it
     */
    public boolean exists(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (documentsById.getValue(id) != null) {
            return true;
        }

        HeadDocumentCommand command = new HeadDocumentCommand(id, null);

        _requestExecutor.execute(command, sessionInfo);

        return command.getResult() != null;
    }

    /**
     * Refreshes the specified entity from Raven server.
     */
    public <T> void refresh(T entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);
        if (documentInfo == null) {
            throw new IllegalStateException("Cannot refresh a transient instance");
        }

        incrementRequestCount();

        GetDocumentsCommand command = new GetDocumentsCommand(new String[]{documentInfo.getId()}, null, false);
        _requestExecutor.execute(command, sessionInfo);

        refreshInternal(entity, command, documentInfo);
    }

    /**
     * Generates the document ID.
     */
    @Override
    protected String generateId(Object entity) {
        return getConventions().generateDocumentId(getDatabaseName(), entity);
    }

    /* TODO

        public ResponseTimeInformation ExecuteAllPendingLazyOperations()
        {
            if (PendingLazyOperations.Count == 0)
                return new ResponseTimeInformation();

            try
            {
                var sw = Stopwatch.StartNew();

                IncrementRequestCount();

                var responseTimeDuration = new ResponseTimeInformation();

                while (ExecuteLazyOperationsSingleStep(responseTimeDuration))
                {
                    Thread.Sleep(100);
                }

                responseTimeDuration.ComputeServerTotal();


                foreach (var pendingLazyOperation in PendingLazyOperations)
                {
                    Action<object> value;
                    if (OnEvaluateLazy.TryGetValue(pendingLazyOperation, out value))
                        value(pendingLazyOperation.Result);
                }
                responseTimeDuration.TotalClientDuration = sw.Elapsed;
                return responseTimeDuration;
            }
            finally
            {
                PendingLazyOperations.Clear();
            }
        }

        private bool ExecuteLazyOperationsSingleStep(ResponseTimeInformation responseTimeInformation)
        {
            //WIP - Not final
            var requests = PendingLazyOperations.Select(x => x.CreateRequest(Context)).ToList();
            var multiGetOperation = new MultiGetOperation(this);
            var multiGetCommand = multiGetOperation.CreateRequest(requests);
            RequestExecutor.Execute(multiGetCommand, Context, sessionInfo: SessionInfo);
            var responses = multiGetCommand.Result;

            for (var i = 0; i < PendingLazyOperations.Count; i++)
            {
                long totalTime;
                string tempReqTime;
                var response = responses[i];

                response.Headers.TryGetValue(Constants.Headers.RequestTime, out tempReqTime);

                long.TryParse(tempReqTime, out totalTime);

                responseTimeInformation.DurationBreakdown.Add(new ResponseTimeItem
                {
                    Url = requests[i].UrlAndQuery,
                    Duration = TimeSpan.FromMilliseconds(totalTime)
                });

                if (response.RequestHasErrors())
                    throw new InvalidOperationException("Got an error from server, status code: " + (int)response.StatusCode + Environment.NewLine + response.Result);

                PendingLazyOperations[i].HandleResponse(response);
                if (PendingLazyOperations[i].RequiresRetry)
                {
                    return true;
                }
            }
            return false;
        }
    }
    */

    /**
     * Begin a load while including the specified path
     */
    public ILoaderWithInclude include(String path) {
        return new MultiLoaderWithInclude(this).include(path);
    }

    //TBD ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, string>> path)
    //TBD ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, IEnumerable<string>>> path)
    //TBD Lazy<Dictionary<string, T>> ILazySessionOperations.Load<T>(IEnumerable<string> ids)
    //TBD Lazy<Dictionary<string, T>> ILazySessionOperations.Load<T>(IEnumerable<string> ids, Action<Dictionary<string, T>> onEval)
    //TBD Lazy<T> ILazySessionOperations.Load<T>(string id)
    //TBD Lazy<T> ILazySessionOperations.Load<T>(string id, Action<T> onEval)
    //TBD internal Lazy<T> AddLazyOperation<T>(ILazyOperation operation, Action<T> onEval)
    //TBD Lazy<Dictionary<string, TResult>> ILazySessionOperations.LoadStartingWith<TResult>(string idPrefix, string matches, int start, int pageSize, string exclude, string startAfter)
    //TBD Lazy<List<TResult>> ILazySessionOperations.MoreLikeThis<TResult>(MoreLikeThisQuery query)
    //TBD ILazyLoaderWithInclude<object> ILazySessionOperations.Include(string path)
    //TBD public Lazy<Dictionary<string, T>> LazyLoadInternal<T>(string[] ids, string[] includes, Action<Dictionary<string, T>> onEval)
    //TBD internal Lazy<int> AddLazyCountOperation(ILazyOperation operation)

    @Override
    public <T> T load(Class<T> clazz, String id) {
        if (id == null) {
            return Defaults.defaultValue(clazz);
        }

        LoadOperation loadOperation = new LoadOperation(this);

        loadOperation.byId(id);

        GetDocumentsCommand command = loadOperation.createRequest();

        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocument(clazz);
    }

    public <T> Map<String, T> load(Class<T> clazz, String... ids) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(clazz, ids, loadOperation);
        return loadOperation.getDocuments(clazz);
    }


    /**
     * Loads the specified entities with the specified ids.
     */
    public <T> Map<String, T> load(Class<T> clazz, Collection<String> ids) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(clazz, ids.toArray(new String[0]), loadOperation);
        return loadOperation.getDocuments(clazz);
    }

    private <T> void loadInternal(Class<T> clazz, String[] ids, LoadOperation operation) { //TBD optional stream parameter
        operation.byIds(ids);

        GetDocumentsCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            /* TBD
             if(stream!=null)
                    Context.Write(stream, command.Result.Results.Parent);
                else
                    operation.SetResult(command.Result);
             */

            operation.setResult(command.getResult()); //TBD: delete me after impl stream
        }

    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadOperation.byIds(ids);
        loadOperation.withIncludes(includes);

        GetDocumentsCommand command = loadOperation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocuments(clazz);
    }

    /* TODO

        public T[] LoadStartingWith<T>(string idPrefix, string matches = null, int start = 0, int pageSize = 25, string exclude = null,
            string startAfter = null)
        {
            var loadStartingWithOperation = new LoadStartingWithOperation(this);
            LoadStartingWithInternal(idPrefix, loadStartingWithOperation, null, matches, start, pageSize, exclude, startAfter);
            return loadStartingWithOperation.GetDocuments<T>();
        }


        public void LoadStartingWithIntoStream(string idPrefix, Stream output, string matches = null, int start = 0, int pageSize = 25, string exclude = null,
            string startAfter = null)
        {
            LoadStartingWithInternal(idPrefix, new LoadStartingWithOperation(this), output, matches, start, pageSize, exclude, startAfter);
        }

        private GetDocumentsCommand LoadStartingWithInternal(string idPrefix, LoadStartingWithOperation operation, Stream stream = null, string matches = null,
            int start = 0, int pageSize = 25, string exclude = null,
            string startAfter = null)
        {
            operation.WithStartWith(idPrefix, matches, start, pageSize, exclude, startAfter);

            var command = operation.CreateRequest();
            if (command != null)
            {
                RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);

                if (stream != null)
                    Context.Write(stream, command.Result.Results.Parent);
                else
                    operation.SetResult(command.Result);
            }

            return command;
        }
        */
    //TBD public void LoadIntoStream(IEnumerable<string> ids, Stream output)
    //TBD public List<T> MoreLikeThis<T, TIndexCreator>(string documentId) where TIndexCreator : AbstractIndexCreationTask, new()
    //TBD public List<T> MoreLikeThis<T, TIndexCreator>(MoreLikeThisQuery query) where TIndexCreator : AbstractIndexCreationTask, new()
    //TBD public List<T> MoreLikeThis<T>(string index, string documentId)
    //TBD public List<T> MoreLikeThis<T>(MoreLikeThisQuery query)

    /* TODO
        private static string CreateQuery(string indexName)
        {
            var fromToken = FromToken.Create(indexName, null);

            var sb = new StringBuilder();
            fromToken.WriteTo(sb);

            return sb.ToString();
        }

        private int _valsCount;
        private int _customCount;
*/

    //TBD public void Increment<T, U>(T entity, Expression<Func<T, U>> path, U valToAdd)
    //TBD public void Increment<T, U>(string id, Expression<Func<T, U>> path, U valToAdd)
    //TBD public void Patch<T, U>(T entity, Expression<Func<T, U>> path, U value)
    //TBD public void Patch<T, U>(string id, Expression<Func<T, U>> path, U value)
    //TBD public void Patch<T, U>(T entity, Expression<Func<T, IEnumerable<U>>> path, Expression<Func<JavaScriptArray<U>, object>> arrayAdder)
    //TBD public void Patch<T, U>(string id, Expression<Func<T, IEnumerable<U>>> path, Expression<Func<JavaScriptArray<U>, object>> arrayAdder)
    //TBD private bool TryMergePatches(string id, PatchRequest patchRequest)

    public <T, TIndex extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<TIndex> indexClazz) {
        try {
            TIndex index = indexClazz.newInstance();
            return documentQuery(clazz, index.getIndexName(), null, index.isMapReduce());
        } catch (IllegalAccessException | IllegalStateException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     * @param indexName Name of the index (mutually exclusive with collectionName)
     * @param collectionName Name of the collection (mutually exclusive with indexName)
     * @param isMapReduce Whether we are querying a map/reduce index (modify how we treat identifier properties)
     * @return
     */
    public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce) {
        return new DocumentQuery<>(clazz, this, indexName, collectionName, isMapReduce);
    }

    public <T> IRawDocumentQuery<T> rawQuery(Class<T> clazz, String query) {
        return new RawDocumentQuery<>(clazz, this, query);
    }

    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query, out StreamQueryStatistics streamQueryStats)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats)
    //TBD private IEnumerator<StreamResult<T>> YieldResults<T>(IDocumentQuery<T> query, IEnumerator<BlittableJsonReaderObject> enumerator)
    //TBD public void StreamInto<T>(IRawDocumentQuery<T> query, Stream output)
    //TBD public void StreamInto<T>(IDocumentQuery<T> query, Stream output)
    //TBD private StreamResult<T> CreateStreamResult<T>(BlittableJsonReaderObject json, string[] projectionFields)
    //TBD public IEnumerator<StreamResult<T>> Stream<T>(string startsWith, string matches = null, int start = 0, int pageSize = int.MaxValue, string startAfter = null)

    /* TODO delete - move?
    public <T> List<T> getRevisionsFor(Class<T> clazz, String id) {
        return getRevisionsFor(clazz, id, 0, 25);
    }

    public <T> List<T> getRevisionsFor(Class<T> clazz, String id, int start) {
        return getRevisionsFor(clazz, id, start, 25);
    }

    public <T> List<T> getRevisionsFor(Class<T> clazz, String id, int start, int pageSize) {
        GetRevisionOperation operation = new GetRevisionOperation(this, id, start, pageSize);

        GetRevisionsCommand command = operation.createRequest();
        getRequestExecutor().execute(command, sessionInfo);
        operation.setResult(command.getResult());
        return operation.complete(clazz);
    }*/

}
