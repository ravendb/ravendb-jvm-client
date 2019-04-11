package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import com.google.common.base.Stopwatch;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.commands.*;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.PatchCommandData;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.commands.multiGet.MultiGetCommand;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.linq.IDocumentQueryGenerator;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.loaders.IIncludeBuilder;
import net.ravendb.client.documents.session.loaders.ILoaderWithInclude;
import net.ravendb.client.documents.session.loaders.IncludeBuilder;
import net.ravendb.client.documents.session.loaders.MultiLoaderWithInclude;
import net.ravendb.client.documents.session.operations.*;
import net.ravendb.client.documents.session.operations.lazy.*;
import net.ravendb.client.documents.session.tokens.FieldsToFetchToken;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    @Override
    public ILazySessionOperations lazily() {
        return new LazySessionOperations(this);
    }

    @Override
    public IEagerSessionOperations eagerly() {
        return this;
    }

    private IAttachmentsSessionOperations _attachments;

    @Override
    public IAttachmentsSessionOperations attachments() {
        if (_attachments == null) {
            _attachments = new DocumentSessionAttachments(this);
        }
        return _attachments;
    }

    private IRevisionsSessionOperations _revisions;

    @Override
    public IRevisionsSessionOperations revisions() {
        if (_revisions == null) {
            _revisions = new DocumentSessionRevisions(this);
        }
        return _revisions;
    }

    private IClusterTransactionOperations _clusterTransaction;

    @Override
    public IClusterTransactionOperations clusterTransaction() {
        if (_clusterTransaction == null) {
            _clusterTransaction = new ClusterTransactionOperations(this);
        }
        return _clusterTransaction;
    }

    @Override
    protected ClusterTransactionOperationsBase getClusterSession() {
        return (ClusterTransactionOperationsBase) _clusterTransaction;
    }

    /**
     * Initializes new DocumentSession
     * @param documentStore Parent document store
     * @param id Identifier
     * @param options SessionOptions
     */
    public DocumentSession(DocumentStore documentStore, UUID id, SessionOptions options) {
        super(documentStore, id, options);
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

            if (noTracking) {
                throw new IllegalStateException("Cannot execute saveChanges when entity tracking is disabled in session.");
            }

            _requestExecutor.execute(command, sessionInfo);
            updateSessionAfterSaveChanges(command.getResult());
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

        if (_knownMissingIds.contains(id)) {
            return false;
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

    public ResponseTimeInformation executeAllPendingLazyOperations() {
        ArrayList<GetRequest> requests = new ArrayList<>();
        for (int i = 0; i < pendingLazyOperations.size(); i++) {
            GetRequest req = pendingLazyOperations.get(i).createRequest();
            if (req == null) {
                pendingLazyOperations.remove(i);
                i--; // so we'll recheck this index
                continue;
            }
            requests.add(req);
        }

        if (requests.isEmpty()) {
            return new ResponseTimeInformation();
        }

        try  {
            Stopwatch sw = Stopwatch.createStarted();

            incrementRequestCount();

            ResponseTimeInformation responseTimeDuration = new ResponseTimeInformation();

            while (executeLazyOperationsSingleStep(responseTimeDuration, requests)) {
                Thread.sleep(100);
            }

            responseTimeDuration.computeServerTotal();

            for (ILazyOperation pendingLazyOperation : pendingLazyOperations) {
                Consumer<Object> value = onEvaluateLazy.get(pendingLazyOperation);
                if (value != null) {
                    value.accept(pendingLazyOperation.getResult());
                }
            }

            responseTimeDuration.setTotalClientDuration(Duration.ofMillis(sw.elapsed(TimeUnit.MILLISECONDS)));
            return responseTimeDuration;
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to execute pending operations: "  + e.getMessage(), e);
        } finally {
            pendingLazyOperations.clear();
        }
    }

    private boolean executeLazyOperationsSingleStep(ResponseTimeInformation responseTimeInformation, List<GetRequest> requests) {

        MultiGetOperation multiGetOperation = new MultiGetOperation(this);
        MultiGetCommand multiGetCommand = multiGetOperation.createRequest(requests);
        getRequestExecutor().execute(multiGetCommand, sessionInfo);

        List<GetResponse> responses = multiGetCommand.getResult();

        for (int i = 0; i < pendingLazyOperations.size(); i++) {
            long totalTime;
            String tempReqTime;
            GetResponse response = responses.get(i);

            tempReqTime = response.getHeaders().get(Constants.Headers.REQUEST_TIME);
            totalTime = tempReqTime != null ? Long.valueOf(tempReqTime) : 0;

            ResponseTimeInformation.ResponseTimeItem timeItem = new ResponseTimeInformation.ResponseTimeItem();
            timeItem.setUrl(requests.get(i).getUrlAndQuery());
            timeItem.setDuration(Duration.ofMillis(totalTime));

            responseTimeInformation.getDurationBreakdown().add(timeItem);

            if (response.requestHasErrors()) {
                throw new IllegalStateException("Got an error from server, status code: " + response.getStatusCode() + System.lineSeparator() + response.getResult());
            }

            pendingLazyOperations.get(i).handleResponse(response);
            if (pendingLazyOperations.get(i).isRequiresRetry()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Begin a load while including the specified path
     */
    public ILoaderWithInclude include(String path) {
        return new MultiLoaderWithInclude(this).include(path);
    }

    public <T> Lazy<T> addLazyOperation(Class<T> clazz, ILazyOperation operation, Consumer<T> onEval) {
        pendingLazyOperations.add(operation);
        Lazy<T> lazyValue = new Lazy<>(() -> {
            executeAllPendingLazyOperations();
            return getOperationResult(clazz, operation.getResult());
        });

        if (onEval != null) {
            onEvaluateLazy.put(operation, theResult -> onEval.accept(getOperationResult(clazz, theResult)));
        }

        return lazyValue;
    }

    protected Lazy<Integer> addLazyCountOperation(ILazyOperation operation) {
        pendingLazyOperations.add(operation);

        return new Lazy<>(() -> {
            executeAllPendingLazyOperations();
            return operation.getQueryResult().getTotalResults();
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Lazy<Map<String, T>> lazyLoadInternal(Class<T> clazz, String[] ids, String[] includes, Consumer<Map<String, T>> onEval) {
        if (checkIfIdAlreadyIncluded(ids, Arrays.asList(includes))) {
            return new Lazy<>(() -> load(clazz, ids));
        }

        LoadOperation loadOperation = new LoadOperation(this)
                .byIds(ids)
                .withIncludes(includes);

        LazyLoadOperation<T> lazyOp = new LazyLoadOperation<>(clazz, this, loadOperation)
                .byIds(ids).withIncludes(includes);

        return addLazyOperation((Class<Map<String, T>>)(Class<?>)Map.class, lazyOp, onEval);
    }

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
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(ids, loadOperation, null);
        return loadOperation.getDocuments(clazz);
    }

    /**
     * Loads the specified entities with the specified ids.
     */
    public <T> Map<String, T> load(Class<T> clazz, Collection<String> ids) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(ids.toArray(new String[0]), loadOperation, null);
        return loadOperation.getDocuments(clazz);
    }

    private <T> void loadInternal(String[] ids, LoadOperation operation, OutputStream stream) {
        operation.byIds(ids);

        GetDocumentsCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);

            if (stream != null) {
                try {
                    GetDocumentsResult result = command.getResult();
                    JsonExtensions.getDefaultMapper().writeValue(stream, result);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to serialize returned value into stream" + e.getMessage(), e);
                }
            } else {
                operation.setResult(command.getResult());
            }
        }
    }

    @Override
    public <T> T load(Class<T> clazz, String id, Consumer<IIncludeBuilder> includes) {
        if (id == null) {
            return null;
        }

        Collection<T> values = load(clazz, Collections.singletonList(id), includes).values();
        return values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids, Consumer<IIncludeBuilder> includes) {
        if (ids == null) {
            throw new IllegalArgumentException("ids cannot be null");
        }

        if (includes == null) {
            return load(clazz, ids);
        }

        IncludeBuilder includeBuilder = new IncludeBuilder(getConventions());
        includes.accept(includeBuilder);

        return loadInternal(clazz,
                ids.toArray(new String[0]),
                includeBuilder.documentsToInclude != null ? includeBuilder.documentsToInclude.toArray(new String[0]) : null,
                includeBuilder.getCountersToInclude() != null ? includeBuilder.getCountersToInclude().toArray(new String[0]) : null,
                includeBuilder.isAllCounters());
    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes) {
        return loadInternal(clazz, ids, includes, null, false);
    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes, String[] counterIncludes) {
        return loadInternal(clazz, ids, includes, counterIncludes, false);
    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes, String[] counterIncludes, boolean includeAllCounters) {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        LoadOperation loadOperation = new LoadOperation(this);
        loadOperation.byIds(ids);
        loadOperation.withIncludes(includes);

        if (includeAllCounters) {
            loadOperation.withAllCounters();
        } else {
            loadOperation.withCounters(counterIncludes);
        }

        GetDocumentsCommand command = loadOperation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocuments(clazz);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix) {
        return loadStartingWith(clazz, idPrefix, null, 0, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches) {
        return loadStartingWith(clazz, idPrefix, matches, 0, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start) {
        return loadStartingWith(clazz, idPrefix, matches, start, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, exclude, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter) {
        LoadStartingWithOperation loadStartingWithOperation = new LoadStartingWithOperation(this);
        loadStartingWithInternal(idPrefix, loadStartingWithOperation, null, matches, start, pageSize, exclude, startAfter);
        return loadStartingWithOperation.getDocuments(clazz);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output) {
        loadStartingWithIntoStream(idPrefix, output, null, 0, 25, null, null);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches) {
        loadStartingWithIntoStream(idPrefix, output, matches, 0, 25, null, null);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start) {
        loadStartingWithIntoStream(idPrefix, output, matches, start, 25, null, null);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize) {
        loadStartingWithIntoStream(idPrefix, output, matches, start, pageSize, null, null);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize, String exclude) {
        loadStartingWithIntoStream(idPrefix, output, matches, start, pageSize, exclude, null);
    }

    @Override
    public void loadStartingWithIntoStream(String idPrefix, OutputStream output, String matches, int start, int pageSize, String exclude, String startAfter) {
        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        }
        if (idPrefix == null) {
            throw new IllegalArgumentException("idPrefix cannot be null");
        }
        loadStartingWithInternal(idPrefix, new LoadStartingWithOperation(this), output, matches, start, pageSize, exclude, startAfter);
    }

    @SuppressWarnings("UnusedReturnValue")
    private GetDocumentsCommand loadStartingWithInternal(String idPrefix, LoadStartingWithOperation operation, OutputStream stream,
                                                         String matches, int start, int pageSize, String exclude, String startAfter) {
        operation.withStartWith(idPrefix, matches, start, pageSize, exclude, startAfter);

        GetDocumentsCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);

            if (stream != null) {
                try {
                    GetDocumentsResult result = command.getResult();
                    JsonExtensions.getDefaultMapper().writeValue(stream, result);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to serialize returned value into stream" + e.getMessage(), e);
                }
            } else {
                operation.setResult(command.getResult());
            }
        }
        return command;
    }

    @Override
    public void loadIntoStream(Collection<String> ids, OutputStream output) {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        loadInternal(ids.toArray(new String[0]), new LoadOperation(this), output);
    }

    @Override
    public <T, U> void increment(T entity, String path, U valueToAdd) {
        IMetadataDictionary metadata = getMetadataFor(entity);
        String id = (String) metadata.get(Constants.Documents.Metadata.ID);
        increment(id, path, valueToAdd);
    }

    private int _valsCount;
    private int _customCount;

    @Override
    public <T, U> void increment(String id, String path, U valueToAdd) {
        PatchRequest patchRequest = new PatchRequest();

        String variable = "this." + path;
        String value = "args.val_" + _valsCount;
        patchRequest.setScript(variable + " = " + variable
                + " ? " + variable + " + " + value
                + " : " + value + ";");
        patchRequest.setValues(Collections.singletonMap("val_" + _valsCount, valueToAdd));

        _valsCount++;

        if (!tryMergePatches(id, patchRequest)) {
            defer(new PatchCommandData(id, null, patchRequest, null));
        }
    }

    @Override
    public <T, U> void patch(T entity, String path, U value) {
        IMetadataDictionary metadata = getMetadataFor(entity);
        String id = (String) metadata.get(Constants.Documents.Metadata.ID);
        patch(id, path, value);
    }

    @Override
    public <T, U> void patch(String id, String path, U value) {

        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setScript("this." + path + " = args.val_" + _valsCount + ";");
        patchRequest.setValues(Collections.singletonMap("val_" + _valsCount, value));

        _valsCount++;

        if (!tryMergePatches(id, patchRequest)) {
            defer(new PatchCommandData(id, null, patchRequest, null));
        }
    }

    @Override
    public <T, U> void patch(T entity, String pathToArray, Consumer<JavaScriptArray<U>> arrayAdder) {
        IMetadataDictionary metadata = getMetadataFor(entity);
        String id = (String) metadata.get(Constants.Documents.Metadata.ID);
        patch(id, pathToArray, arrayAdder);
    }

    @Override
    public <T, U> void patch(String id, String pathToArray, Consumer<JavaScriptArray<U>> arrayAdder) {
        JavaScriptArray<U> scriptArray = new JavaScriptArray<>(_customCount++, pathToArray);

        arrayAdder.accept(scriptArray);

        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setScript(scriptArray.getScript());
        patchRequest.setValues(scriptArray.getParameters());

        if (!tryMergePatches(id, patchRequest)) {
            defer(new PatchCommandData(id, null, patchRequest, null));
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryMergePatches(String id, PatchRequest patchRequest) {
        ICommandData command = deferredCommandsMap.get(IdTypeAndName.create(id, CommandType.PATCH, null));
        if (command == null) {
            return false;
        }

        deferredCommands.remove(command);
        // We'll overwrite the deferredCommandsMap when calling Defer
        // No need to call deferredCommandsMap.remove((id, CommandType.PATCH, null));

        PatchCommandData oldPatch = (PatchCommandData) command;
        String newScript = oldPatch.getPatch().getScript() + "\n" + patchRequest.getScript();
        Map<String, Object> newVals = new HashMap<>(oldPatch.getPatch().getValues());

        for (Map.Entry<String, Object> kvp : patchRequest.getValues().entrySet()) {
            newVals.put(kvp.getKey(), kvp.getValue());
        }

        PatchRequest newPatchRequest = new PatchRequest();
        newPatchRequest.setScript(newScript);
        newPatchRequest.setValues(newVals);

        defer(new PatchCommandData(id, null, newPatchRequest, null));

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public <T, TIndex extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<TIndex> indexClazz) {
        try {
            TIndex index = indexClazz.newInstance();
            return documentQuery(clazz, index.getIndexName(), null, index.isMapReduce());
        } catch (IllegalAccessException | IllegalStateException | InstantiationException e) {
            throw new RuntimeException("Unable to query index: " + indexClazz.getSimpleName() + e.getMessage(), e);
        }
    }

    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     */
    public <T> IDocumentQuery<T> documentQuery(Class<T> clazz) {
        return documentQuery(clazz, null, null, false);
    }

    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     * @param indexName Name of the index (mutually exclusive with collectionName)
     * @param collectionName Name of the collection (mutually exclusive with indexName)
     * @param isMapReduce Whether we are querying a map/reduce index (modify how we treat identifier properties)
     */
    public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce) {
        Tuple<String, String> indexNameAndCollection = processQueryParameters(clazz, indexName, collectionName, getConventions());
        indexName = indexNameAndCollection.first;
        collectionName = indexNameAndCollection.second;

        return new DocumentQuery<>(clazz, this, indexName, collectionName, isMapReduce);
    }

    @Override
    public InMemoryDocumentSessionOperations getSession() {
        return this;
    }

    public <T> IRawDocumentQuery<T> rawQuery(Class<T> clazz, String query) {
        return new RawDocumentQuery<>(clazz, this, query);
    }

    @Override
    public <T> IDocumentQuery<T> query(Class<T> clazz) {
        return documentQuery(clazz, null, null, false);
    }

    @Override
    public <T> IDocumentQuery<T> query(Class<T> clazz, Query collectionOrIndexName) {
        if (StringUtils.isNotEmpty(collectionOrIndexName.getCollection())) {
            return documentQuery(clazz, null, collectionOrIndexName.getCollection(), false);
        }

        return documentQuery(clazz, collectionOrIndexName.getIndexName(), null, false);
    }

    @Override
    public <T, TIndex extends AbstractIndexCreationTask> IDocumentQuery<T> query(Class<T> clazz, Class<TIndex> indexClazz) {
        return documentQuery(clazz, indexClazz);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query) {
        StreamOperation streamOperation = new StreamOperation(this);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        CloseableIterator<ObjectNode> result = streamOperation.setResult(command.getResult());
        return yieldResults((AbstractDocumentQuery) query, result);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query, Reference<StreamQueryStatistics> streamQueryStats) {
        StreamQueryStatistics stats = new StreamQueryStatistics();
        StreamOperation streamOperation = new StreamOperation(this, stats);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        CloseableIterator<ObjectNode> result = streamOperation.setResult(command.getResult());
        streamQueryStats.value = stats;

        return yieldResults((AbstractDocumentQuery)query, result);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(IRawDocumentQuery<T> query) {
        StreamOperation streamOperation = new StreamOperation(this);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        CloseableIterator<ObjectNode> result = streamOperation.setResult(command.getResult());
        return yieldResults((AbstractDocumentQuery) query, result);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(IRawDocumentQuery<T> query, Reference<StreamQueryStatistics> streamQueryStats) {
        StreamQueryStatistics stats = new StreamQueryStatistics();
        StreamOperation streamOperation = new StreamOperation(this, stats);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        CloseableIterator<ObjectNode> result = streamOperation.setResult(command.getResult());
        streamQueryStats.value = stats;

        return yieldResults((AbstractDocumentQuery) query, result);
    }

    @SuppressWarnings("unchecked")
    private <T> CloseableIterator<StreamResult<T>> yieldResults(AbstractDocumentQuery query, CloseableIterator<ObjectNode> enumerator) {
        return new StreamIterator<>(query.getQueryClass(), enumerator, query.fieldsToFetchToken, query::invokeAfterStreamExecuted);
    }

    @Override
    public <T> void streamInto(IRawDocumentQuery<T> query, OutputStream output) {
        StreamOperation streamOperation = new StreamOperation(this);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        try {
            IOUtils.copy(command.getResult().getStream(), output);
        } catch (IOException e) {
            throw new RuntimeException("Unable to stream results into OutputStream: " + e.getMessage(), e);
        } finally {
            EntityUtils.consumeQuietly(command.getResult().getResponse().getEntity());
        }
    }

    @Override
    public <T> void streamInto(IDocumentQuery<T> query, OutputStream output) {
        StreamOperation streamOperation = new StreamOperation(this);
        QueryStreamCommand command = streamOperation.createRequest(query.getIndexQuery());

        getRequestExecutor().execute(command, sessionInfo);

        try {
            IOUtils.copy(command.getResult().getStream(), output);
        } catch (IOException e) {
            throw new RuntimeException("Unable to stream results into OutputStream: " + e.getMessage(), e);
        } finally {
            EntityUtils.consumeQuietly(command.getResult().getResponse().getEntity());
        }
    }

    private <T> StreamResult<T> createStreamResult(Class<T> clazz, ObjectNode json, FieldsToFetchToken fieldsToFetch) throws IOException {

        ObjectNode metadata = (ObjectNode) json.get(Constants.Documents.Metadata.KEY);
        String changeVector = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR).asText();
        // MapReduce indexes return reduce results that don't have @id property
        String id = null;
        JsonNode idJson = metadata.get(Constants.Documents.Metadata.ID);
        if (idJson != null && !idJson.isNull()) {
            id = idJson.asText();
        }


        T entity = QueryOperation.deserialize(clazz, id, json, metadata, fieldsToFetch, true, this);

        StreamResult<T> streamResult = new StreamResult<>();
        streamResult.setChangeVector(changeVector);
        streamResult.setId(id);
        streamResult.setDocument(entity);
        streamResult.setMetadata(new MetadataAsDictionary(metadata));

        return streamResult;
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith) {
        return stream(clazz, startsWith, null, 0, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches) {
        return stream(clazz, startsWith, matches, 0, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start) {
        return stream(clazz, startsWith, matches, start, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start, int pageSize) {
        return stream(clazz, startsWith, matches, start, pageSize, null);
    }

    @Override
    public <T> CloseableIterator<StreamResult<T>> stream(Class<T> clazz, String startsWith, String matches, int start, int pageSize, String startAfter) {
        StreamOperation streamOperation = new StreamOperation(this);

        StreamCommand command = streamOperation.createRequest(startsWith, matches, start, pageSize, null, startAfter);
        getRequestExecutor().execute(command, sessionInfo);

        CloseableIterator<ObjectNode> result = streamOperation.setResult(command.getResult());
        return new StreamIterator<>(clazz, result, null, null);
    }

    private class StreamIterator<T> implements CloseableIterator<StreamResult<T>> {

        private final Class<T> _clazz;
        private final CloseableIterator<ObjectNode> _innerIterator;
        private final FieldsToFetchToken _fieldsToFetchToken;
        private final Consumer<ObjectNode> _onNextItem;

        public StreamIterator(Class<T> clazz, CloseableIterator<ObjectNode> innerIterator, FieldsToFetchToken fieldsToFetchToken, Consumer<ObjectNode> onNextItem) {
            _clazz = clazz;
            _innerIterator = innerIterator;
            _fieldsToFetchToken = fieldsToFetchToken;
            _onNextItem = onNextItem;
        }

        @Override
        public boolean hasNext() {
            return _innerIterator.hasNext();
        }

        @Override
        public StreamResult<T> next() {
            ObjectNode nextValue = _innerIterator.next();
            try {
                if (_onNextItem != null) {
                    _onNextItem.accept(nextValue);
                }
                return createStreamResult(_clazz, nextValue, _fieldsToFetchToken);
            } catch (IOException e) {
                throw new RuntimeException("Unable to parse stream result: " + e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            _innerIterator.close();
        }
    }

    @Override
    public ISessionDocumentCounters countersFor(String documentId) {
        return new SessionDocumentCounters(this, documentId);
    }

    @Override
    public ISessionDocumentCounters countersFor(Object entity) {
        return new SessionDocumentCounters(this, entity);
    }

}
