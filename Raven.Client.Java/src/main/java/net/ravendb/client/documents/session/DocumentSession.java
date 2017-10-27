package net.ravendb.client.documents.session;

import com.google.common.base.Defaults;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.GetDocumentCommand;
import net.ravendb.client.documents.commands.HeadDocumentCommand;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.linq.IDocumentQueryGenerator;
import net.ravendb.client.documents.session.loaders.ILoaderWithInclude;
import net.ravendb.client.documents.session.loaders.MultiLoaderWithInclude;
import net.ravendb.client.documents.session.operations.BatchOperation;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;
import net.ravendb.client.documents.session.operations.lazy.ILazySessionOperations;
import net.ravendb.client.http.RequestExecutor;

import java.util.Collection;
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
    @Override
    public ILazySessionOperations lazily() {
        return this; //TODO: return LazySessionOperations here - see old client!
    }

    /**
     * Access the eager operations
     */
    @Override
    public IEagerSessionOperations eagerly() {
        return this;
    }

    /**
     * Initializes new DocumentSession
     */
    public DocumentSession(String dbName, DocumentStore documentStore, UUID id, RequestExecutor requestExecutor) {
        super(dbName, documentStore, requestExecutor, id);
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

        GetDocumentCommand command = new GetDocumentCommand(new String[]{documentInfo.getId()}, null, false);
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

    /* TODO


         /// <summary>
        /// Begin a load while including the specified path
        /// </summary>
        /// <param name="path">The path.</param>
        ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, string>> path)
        {
            return new LazyMultiLoaderWithInclude<T>(this).Include(path);
        }

        /// <summary>
        /// Begin a load while including the specified path
        /// </summary>
        /// <param name="path">The path.</param>
        ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, IEnumerable<string>>> path)
        {
            return new LazyMultiLoaderWithInclude<T>(this).Include(path);
        }

        /// <summary>
        /// Loads the specified ids.
        /// </summary>
        Lazy<Dictionary<string, T>> ILazySessionOperations.Load<T>(IEnumerable<string> ids)
        {
            return Lazily.Load<T>(ids, null);
        }

        /// <summary>
        /// Loads the specified ids and a function to call when it is evaluated
        /// </summary>
        Lazy<Dictionary<string, T>> ILazySessionOperations.Load<T>(IEnumerable<string> ids, Action<Dictionary<string, T>> onEval)
        {
            return LazyLoadInternal(ids.ToArray(), new string[0], onEval);
        }

        /// <summary>
        /// Loads the specified id.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="id">The id.</param>
        /// <returns></returns>
        Lazy<T> ILazySessionOperations.Load<T>(string id)
        {
            return Lazily.Load(id, (Action<T>)null);
        }

        /// <summary>
        /// Loads the specified id and a function to call when it is evaluated
        /// </summary>
        Lazy<T> ILazySessionOperations.Load<T>(string id, Action<T> onEval)
        {
            if (IsLoaded(id))
                return new Lazy<T>(() => Load<T>(id));
            //TODO - DisableAllCaching
            var lazyLoadOperation = new LazyLoadOperation<T>(this, new LoadOperation(this).ById(id)).ById(id);
            return AddLazyOperation(lazyLoadOperation, onEval);
        }

        internal Lazy<T> AddLazyOperation<T>(ILazyOperation operation, Action<T> onEval)
        {
            PendingLazyOperations.Add(operation);
            var lazyValue = new Lazy<T>(() =>
            {
                ExecuteAllPendingLazyOperations();
                return GetOperationResult<T>(operation.Result);
            });

            if (onEval != null)
                OnEvaluateLazy[operation] = theResult => onEval(GetOperationResult<T>(theResult));

            return lazyValue;
        }

        Lazy<Dictionary<string, TResult>> ILazySessionOperations.LoadStartingWith<TResult>(string idPrefix, string matches, int start, int pageSize, string exclude, string startAfter)
        {
            var operation = new LazyStartsWithOperation<TResult>(idPrefix, matches, exclude, start, pageSize, this, startAfter);

            return AddLazyOperation<Dictionary<string, TResult>>(operation, null);
        }

        Lazy<List<TResult>> ILazySessionOperations.MoreLikeThis<TResult>(MoreLikeThisQuery query)
        {
            //TODO - DisableAllCaching
            var lazyOp = new LazyMoreLikeThisOperation<TResult>(this, query);
            return AddLazyOperation<List<TResult>>(lazyOp, null);
        }

        /// <summary>
        /// Begin a load while including the specified path
        /// </summary>
        /// <param name="path">The path.</param>
        ILazyLoaderWithInclude<object> ILazySessionOperations.Include(string path)
        {
            return new LazyMultiLoaderWithInclude<object>(this).Include(path);
        }

        /// <summary>
        /// Register to lazily load documents and include
        /// </summary>
        public Lazy<Dictionary<string, T>> LazyLoadInternal<T>(string[] ids, string[] includes, Action<Dictionary<string, T>> onEval)
        {
            if (CheckIfIdAlreadyIncluded(ids, includes))
            {
                return new Lazy<Dictionary<string, T>>(() => ids.ToDictionary(x => x, Load<T>));
            }
            var loadOperation = new LoadOperation(this)
                .ByIds(ids)
                .WithIncludes(includes);

            var lazyOp = new LazyLoadOperation<T>(this, loadOperation).ByIds(ids).WithIncludes(includes);
            return AddLazyOperation(lazyOp, onEval);
        }

        internal Lazy<int> AddLazyCountOperation(ILazyOperation operation)
        {
            PendingLazyOperations.Add(operation);
            var lazyValue = new Lazy<int>(() =>
            {
                ExecuteAllPendingLazyOperations();
                return operation.QueryResult.TotalResults;
            });

            return lazyValue;
        }

    */

    @Override
    public <T> T load(Class<T> clazz, String id) {
        if (id == null) {
            return Defaults.defaultValue(clazz);
        }

        LoadOperation loadOperation = new LoadOperation(this);

        loadOperation.byId(id);

        GetDocumentCommand command = loadOperation.createRequest();

        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocument(clazz);
    }


    /**
     * Loads the specified entities with the specified ids.
     */
    public <T> Map<String, T> load(Class<T> clazz, Collection<String> ids) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(clazz, ids.toArray(new String[0]), loadOperation);
        return loadOperation.getDocuments(clazz);
    }

    private <T> void loadInternal(Class<T> clazz, String[] ids, LoadOperation operation) { //TODO optional stream parameter
        operation.byIds(ids);

        GetDocumentCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            /* TODO
             if(stream!=null)
                    Context.Write(stream, command.Result.Results.Parent);
                else
                    operation.SetResult(command.Result);
             */

            operation.setResult(command.getResult()); //TODO: delete me after impl stream
        }

    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadOperation.byIds(ids);
        loadOperation.withIncludes(includes);

        GetDocumentCommand command = loadOperation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocuments(clazz);
    }

    /*

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

        private GetDocumentCommand LoadStartingWithInternal(string idPrefix, LoadStartingWithOperation operation, Stream stream = null, string matches = null,
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

        public void LoadIntoStream(IEnumerable<string> ids, Stream output)
        {
            LoadInternal(ids.ToArray(), new LoadOperation(this), output);
        }

          public List<T> MoreLikeThis<T, TIndexCreator>(string documentId) where TIndexCreator : AbstractIndexCreationTask, new()
        {
            if (documentId == null)
                throw new ArgumentNullException(nameof(documentId));

            var index = new TIndexCreator();
            return MoreLikeThis<T>(new MoreLikeThisQuery { Query = CreateQuery(index.IndexName), DocumentId = documentId });
        }

        public List<T> MoreLikeThis<T, TIndexCreator>(MoreLikeThisQuery query) where TIndexCreator : AbstractIndexCreationTask, new()
        {
            if (query == null)
                throw new ArgumentNullException(nameof(query));

            var index = new TIndexCreator();
            query.Query = CreateQuery(index.IndexName);

            return MoreLikeThis<T>(query);
        }

        public List<T> MoreLikeThis<T>(string index, string documentId)
        {
            if (index == null) throw new ArgumentNullException(nameof(index));
            if (documentId == null)
                throw new ArgumentNullException(nameof(documentId));

            return MoreLikeThis<T>(new MoreLikeThisQuery { Query = CreateQuery(index), DocumentId = documentId });
        }

        public List<T> MoreLikeThis<T>(MoreLikeThisQuery query)
        {
            if (query == null)
                throw new ArgumentNullException(nameof(query));

            var operation = new MoreLikeThisOperation(this, query);

            var command = operation.CreateRequest();
            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);

            var result = command.Result;
            operation.SetResult(result);

            return operation.Complete<T>();
        }

        private static string CreateQuery(string indexName)
        {
            var fromToken = FromToken.Create(indexName, null);

            var sb = new StringBuilder();
            fromToken.WriteTo(sb);

            return sb.ToString();
        }


        private int _valsCount;
        private int _customCount;

        public void Increment<T, U>(T entity, Expression<Func<T, U>> path, U valToAdd)
        {
            var metadata = GetMetadataFor(entity);
            var id = metadata.GetString(Constants.Documents.Metadata.Id);
            Increment(id, path, valToAdd);
        }

        public void Increment<T, U>(string id, Expression<Func<T, U>> path, U valToAdd)
        {
            var pathScript = path.CompileToJavascript();

            var patchRequest = new PatchRequest
            {
                Script = $"this.{pathScript} += args.val_{_valsCount};",
                Values = {[$"val_{_valsCount}"] = valToAdd}
            };

            _valsCount++;

            if (TryMergePatches(id, patchRequest) == false)
            {
                Advanced.Defer(new PatchCommandData(id, null, patchRequest, null));
            }
        }

        public void Patch<T, U>(T entity, Expression<Func<T, U>> path, U value)
        {
            var metadata = GetMetadataFor(entity);
            var id = metadata.GetString(Constants.Documents.Metadata.Id);
            Patch(id, path, value);
        }

        public void Patch<T, U>(string id, Expression<Func<T, U>> path, U value)
        {
            var pathScript = path.CompileToJavascript();

            var patchRequest = new PatchRequest
            {
                Script = $"this.{pathScript} = args.val_{_valsCount};",
                Values = {[$"val_{_valsCount}"] = value}
            };

            _valsCount++;

            if (TryMergePatches(id, patchRequest) == false)
            {
                Advanced.Defer(new PatchCommandData(id, null, patchRequest, null));
            }
        }

        public void Patch<T, U>(T entity, Expression<Func<T, IEnumerable<U>>> path,
            Expression<Func<JavaScriptArray<U>, object>> arrayAdder)
        {
            var metadata = GetMetadataFor(entity);
            var id = metadata.GetString(Constants.Documents.Metadata.Id);
            Patch(id, path, arrayAdder);
        }

        public void Patch<T, U>(string id, Expression<Func<T, IEnumerable<U>>> path,
            Expression<Func<JavaScriptArray<U>, object>> arrayAdder)
        {
            var extension = new JavascriptConversionExtensions.CustomMethods
            {
                Suffix = _customCount++
            };
            var pathScript = path.CompileToJavascript();
            var adderScript = arrayAdder.CompileToJavascript(
                new JavascriptCompilationOptions(
                    JsCompilationFlags.BodyOnly | JsCompilationFlags.ScopeParameter,
                    new LinqMethods(), extension));

            var patchRequest = new PatchRequest
            {
                Script = $"this.{pathScript}{adderScript}",
                Values = extension.Parameters
            };

            if (TryMergePatches(id, patchRequest) == false)
            {
                Advanced.Defer(new PatchCommandData(id, null, patchRequest, null));
            }
        }

        private bool TryMergePatches(string id, PatchRequest patchRequest)
        {
            if (DeferredCommandsDictionary.TryGetValue((id, CommandType.PATCH, null), out ICommandData command) == false)
                return false;

            DeferredCommands.Remove(command);
            // We'll overwrite the DeferredCommandsDictionary when calling Defer
            // No need to call DeferredCommandsDictionary.Remove((id, CommandType.PATCH, null));

            var oldPatch = (PatchCommandData)command;
            var newScript = oldPatch.Patch.Script + '\n' + patchRequest.Script;
            var newVals = oldPatch.Patch.Values;

            foreach (var kvp in patchRequest.Values)
            {
                newVals[kvp.Key] = kvp.Value;
            }

            Advanced.Defer(new PatchCommandData(id, null, new PatchRequest
            {
                Script = newScript,
                Values = newVals
            }, null));

            return true;
        }

          /// <summary>
        /// Queries the index specified by <typeparamref name="TIndexCreator"/> using lucene syntax.
        /// </summary>
        /// <typeparam name="T">The result of the query</typeparam>
        /// <typeparam name="TIndexCreator">The type of the index creator.</typeparam>
        /// <returns></returns>
        public IDocumentQuery<T> DocumentQuery<T, TIndexCreator>() where TIndexCreator : AbstractIndexCreationTask, new()
        {
            var index = new TIndexCreator();
            return DocumentQuery<T>(index.IndexName, null, index.IsMapReduce);
        }

        /// <summary>
        /// Query the specified index using Lucene syntax
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="indexName">Name of the index (mutually exclusive with collectionName)</param>
        /// <param name="collectionName">Name of the collection (mutually exclusive with indexName)</param>
        /// <param name="isMapReduce">Whether we are querying a map/reduce index (modify how we treat identifier properties)</param>
        public IDocumentQuery<T> DocumentQuery<T>(string indexName = null, string collectionName = null, bool isMapReduce = false)
        {
            (indexName, collectionName) = ProcessQueryParameters(typeof(T), indexName, collectionName, Conventions);

            return new DocumentQuery<T>(this, indexName, collectionName, isGroupBy: isMapReduce);
        }

        public RavenQueryInspector<S> CreateRavenQueryInspector<S>()
        {
            return new RavenQueryInspector<S>();
        }


        /// <summary>
        /// Create a new query for <typeparam name="T"/>
        /// </summary>
        IDocumentQuery<T> IDocumentQueryGenerator.Query<T>(string indexName, string collectionName, bool isMapReduce)
        {
            return Advanced.DocumentQuery<T>(indexName, collectionName, isMapReduce);
        }

          public IRawDocumentQuery<T> RawQuery<T>(string query)
        {
            var documentQuery = new DocumentQuery<T>(this, null, null, false);
            documentQuery.RawQuery(query);
            return documentQuery;
        }

          public IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query)
        {
            var queryProvider = (IRavenQueryProvider)query.Provider;
            var docQuery = queryProvider.ToDocumentQuery<T>(query.Expression);
            return Stream(docQuery);
        }

        public IEnumerator<StreamResult<T>> Stream<T>(IQueryable<T> query, out StreamQueryStatistics streamQueryStats)
        {
            var queryProvider = (IRavenQueryProvider)query.Provider;
            var docQuery = queryProvider.ToDocumentQuery<T>(query.Expression);
            return Stream(docQuery, out streamQueryStats);
        }

        public IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query)
        {
            var streamOperation = new StreamOperation(this);
            var command = streamOperation.CreateRequest(query.GetIndexQuery());

            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);
            using (var result = streamOperation.SetResult(command.Result))
            {
                return YieldResults(query, result);
            }
        }

        public IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query)
        {
            return Stream((IDocumentQuery<T>)query);
        }

        public IEnumerator<StreamResult<T>> Stream<T>(IRawDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats)
        {
            return Stream((IDocumentQuery<T>)query, out streamQueryStats);
        }

        public IEnumerator<StreamResult<T>> Stream<T>(IDocumentQuery<T> query, out StreamQueryStatistics streamQueryStats)
        {
            var stats = new StreamQueryStatistics();
            var streamOperation = new StreamOperation(this, stats);
            var command = streamOperation.CreateRequest(query.GetIndexQuery());

            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);
            using (var result = streamOperation.SetResult(command.Result))
            {
                streamQueryStats = stats;

                return YieldResults(query, result);
            }
        }

        private IEnumerator<StreamResult<T>> YieldResults<T>(IDocumentQuery<T> query, IEnumerator<BlittableJsonReaderObject> enumerator)
        {
            var projections = ((DocumentQuery<T>)query).FieldsToFetchToken?.Projections;

            while (enumerator.MoveNext())
            {
                var json = enumerator.Current;
                query.InvokeAfterStreamExecuted(json);

                yield return CreateStreamResult<T>(json, projections);
            }
        }

        public void StreamInto<T>(IRawDocumentQuery<T> query, Stream output)
        {
            StreamInto((IDocumentQuery<T>)query, output);
        }

        public void StreamInto<T>(IDocumentQuery<T> query, Stream output)
        {
            var streamOperation = new StreamOperation(this);
            var command = streamOperation.CreateRequest(query.GetIndexQuery());

            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);

            using (command.Result.Response)
            using (command.Result.Stream)
            {
                command.Result.Stream.CopyTo(output);
            }
        }

        private StreamResult<T> CreateStreamResult<T>(BlittableJsonReaderObject json, string[] projectionFields)
        {
            var metadata = json.GetMetadata();
            var changeVector = BlittableJsonExtensions.GetChangeVector(metadata);
            string id;
            //MapReduce indexes return reduce results that don't have @id property
            metadata.TryGetId(out id);

            //TODO - Investigate why ConvertToEntity fails if we don't call ReadObject before
            json = Context.ReadObject(json, id);
            var entity = QueryOperation.Deserialize<T>(id, json, metadata, projectionFields, true, this);

            var streamResult = new StreamResult<T>
            {
                ChangeVector = changeVector,
                Id = id,
                Document = entity,
                Metadata = new MetadataAsDictionary(metadata)
            };
            return streamResult;
        }

        public IEnumerator<StreamResult<T>> Stream<T>(string startsWith, string matches = null, int start = 0, int pageSize = int.MaxValue,
             string startAfter = null)
        {
            var streamOperation = new StreamOperation(this);

            var command = streamOperation.CreateRequest( startsWith, matches, start, pageSize, null, startAfter);
            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);
            using (var result = streamOperation.SetResult(command.Result))
            {
                while (result.MoveNext())
                {
                    var json = result.Current;

                    yield return CreateStreamResult<T>(json, null);
                }
            }
        }

         public List<T> GetRevisionsFor<T>(string id, int start = 0, int pageSize = 25)
        {
            var operation = new GetRevisionOperation(this, id, start, pageSize);

            var command = operation.CreateRequest();
            RequestExecutor.Execute(command, Context, sessionInfo: SessionInfo);
            operation.SetResult(command.Result);
            return operation.Complete<T>();
        }
     */
}
