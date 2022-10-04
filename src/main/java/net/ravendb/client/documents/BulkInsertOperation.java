package net.ravendb.client.documents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.bulkInsert.BulkInsertOptions;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.commands.KillOperationCommand;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.operations.BulkInsertObserver;
import net.ravendb.client.documents.operations.BulkInsertProgress;
import net.ravendb.client.documents.operations.GetOperationStateOperation;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.documents.timeSeries.TimeSeriesOperations;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.documents.bulkinsert.BulkInsertAbortedException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class BulkInsertOperation implements CleanCloseable {

    private BulkInsertOptions _options;
    private String _database;
    private final GenerateEntityIdOnTheClient _generateEntityIdOnTheClient;

    private static class StreamExposerContent extends AbstractHttpEntity {

        public final CompletableFuture<OutputStream> outputStream;
        private final CompletableFuture<Void> _done;

        @SuppressWarnings("unchecked")
        public StreamExposerContent() {
            setContentType(ContentType.APPLICATION_JSON.toString());
            outputStream = new CompletableFuture<>();
            _done = new CompletableFuture();
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            throw new UnsupportedEncodingException();
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public boolean isStreaming() {
            return false;
        }


        public boolean isDone() {
            return _done.isDone();
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public boolean isChunked() {
            return true;
        }

        @SuppressWarnings("SameReturnValue")
        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public void writeTo(OutputStream outputStream) {
            this.outputStream.complete(outputStream);
            try {
                _done.get();
            } catch (Exception e) {
                throw ExceptionsUtils.unwrapException(e);
            }
        }

        public void done() {
            _done.complete(null);
        }

        public void errorOnProcessingRequest(Exception exception) {
            _done.completeExceptionally(exception);
        }

        public void errorOnRequestStart(Exception exception) {
            outputStream.completeExceptionally(exception);
        }
    }

    private static class BulkInsertCommand extends RavenCommand<CloseableHttpResponse> {

        @Override
        public boolean isReadRequest() {
            return false;
        }

        private final StreamExposerContent _stream;

        private boolean _skipOverwriteIfUnchanged;
        private final long _id;

        private boolean useCompression;

        public BulkInsertCommand(long id, StreamExposerContent stream, String nodeTag, boolean skipOverwriteIfUnchanged) {
            super(CloseableHttpResponse.class);
            _stream = stream;
            _id = id;
            this.selectedNodeTag = nodeTag;
            this._skipOverwriteIfUnchanged = skipOverwriteIfUnchanged;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl()
                    + "/databases/"
                    + node.getDatabase()
                    + "/bulk_insert?id=" + _id
                    + "&skipOverwriteIfUnchanged=" + (_skipOverwriteIfUnchanged ? "true" : "false");

            HttpPost message = new HttpPost();
            message.setEntity(useCompression ? new GzipCompressingEntity(_stream) : _stream);
            return message;
        }

        @Override
        public void setResponse(String response, boolean fromCache) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public CloseableHttpResponse send(CloseableHttpClient client, HttpRequestBase request) throws IOException {
            try {
                return super.send(client, request);
            } catch (Exception e) {
                _stream.errorOnRequestStart(e);
                throw e;
            }
        }

        public boolean isUseCompression() {
            return useCompression;
        }

        public void setUseCompression(boolean useCompression) {
            this.useCompression = useCompression;
        }
    }

    private ExecutorService _executorService;
    private final RequestExecutor _requestExecutor;
    private CompletableFuture<Void> _bulkInsertExecuteTask;
    private final ObjectMapper objectMapper;

    private OutputStream _stream;
    private final StreamExposerContent _streamExposerContent;

    private boolean _first = true;
    private CommandType _inProgressCommand;
    private final CountersBulkInsertOperation _countersOperation;
    private final AttachmentsBulkInsertOperation _attachmentsOperation;
    private long _operationId = -1;
    private String _nodeTag;

    private boolean useCompression = false;
    private final int _timeSeriesBatchSize;

    private final AtomicInteger _concurrentCheck = new AtomicInteger();
    private boolean _isInitialWrite = true;

    private CleanCloseable _unsubscribeChanges;
    private final List<EventHandler<BulkInsertOnProgressEventArgs>> _onProgress = new ArrayList<>();
    private boolean _onProgressInitialized = false;

    public BulkInsertOperation(String database, DocumentStore store) {
        this(database, store, null);
    }

    public BulkInsertOperation(String database, DocumentStore store, BulkInsertOptions options) {
        _executorService = store.getExecutorService();
        _conventions = store.getConventions();
        _store = store;
        if (StringUtils.isBlank(database)) {
            throwNoDatabase();
        }

        this.useCompression = options != null ? options.isUseCompression() : false;

        _options = ObjectUtils.firstNonNull(options, new BulkInsertOptions());
        _database = database;
        _requestExecutor = store.getRequestExecutor(database);
        objectMapper = store.getConventions().getEntityMapper();

        _currentWriterBacking = new ByteArrayOutputStream();
        _currentWriter = new OutputStreamWriter(_currentWriterBacking);
        _backgroundWriterBacking = new ByteArrayOutputStream();
        _backgroundWriter = new OutputStreamWriter(_backgroundWriterBacking);
        _streamExposerContent = new StreamExposerContent();
        _countersOperation = new CountersBulkInsertOperation(this);
        _attachmentsOperation = new AttachmentsBulkInsertOperation(this);

        _timeSeriesBatchSize = _conventions.bulkInsert().getTimeSeriesBatchSize();

        _generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(),
                entity -> _requestExecutor.getConventions().generateDocumentId(database, entity));
    }

    public void addOnProgress(EventHandler<BulkInsertOnProgressEventArgs> handler) {
        this._onProgress.add(handler);
        _onProgressInitialized = true;
    }

    public void removeOnProgress(EventHandler<BulkInsertOnProgressEventArgs> handler) {
        this._onProgress.remove(handler);
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    private void throwBulkInsertAborted(Exception e, Exception flushEx) {

        BulkInsertAbortedException errorFromServer = null;
        try {
            errorFromServer = getExceptionFromOperation();
        } catch (Exception ee) {
            // server is probably down, will propagate the original exception
        }

        if (errorFromServer != null) {
            throw errorFromServer;
        }

        throw new BulkInsertAbortedException("Failed to execute bulk insert",
                ObjectUtils.firstNonNull(e, flushEx));
    }

    private void throwNoDatabase() {
        throw new IllegalStateException("Cannot start bulk insert operation without specifying a name of a database to operate on."
            + "Database name can be passed as an argument when bulk insert is being created or default database can be defined using 'DocumentStore.setDatabase' method.");
    }

    private void waitForId() {
        if (_operationId != -1) {
            return;
        }

        GetNextOperationIdCommand bulkInsertGetIdRequest = new GetNextOperationIdCommand();
        _requestExecutor.execute(bulkInsertGetIdRequest);
        _operationId = bulkInsertGetIdRequest.getResult();
        _nodeTag = bulkInsertGetIdRequest.getNodeTag();

        if (_onProgressInitialized && _unsubscribeChanges == null) {
            _unsubscribeChanges = _store.changes()
                    .forOperationId(_operationId)
                    .subscribe(new BulkInsertObserver(this, _conventions));
        }
    }

    public void invokeOnProgress(BulkInsertProgress progress) {
        EventHelper.invoke(_onProgress, this, new BulkInsertOnProgressEventArgs(progress));
    }

    @SuppressWarnings("UnusedReturnValue")
    public String store(Object entity) {
        return store(entity, (IMetadataDictionary) null);
    }


    public String store(Object entity, IMetadataDictionary metadata) {
        String id;
        if (metadata == null || !metadata.containsKey(Constants.Documents.Metadata.ID)) {
            id = getId(entity);
        } else {
            id = (String) metadata.get(Constants.Documents.Metadata.ID);
        }

        store(entity, id, metadata);

        return id;
    }

    public void store(Object entity, String id)  {
        store(entity, id, null);
    }

    public void store(Object entity, String id, IMetadataDictionary metadata) {
        try (CleanCloseable check = concurrencyCheck()) {
            verifyValidId(id);

            executeBeforeStore();

            if (metadata == null) {
                metadata = new MetadataAsDictionary();
            }

            if (!metadata.containsKey(Constants.Documents.Metadata.COLLECTION)) {
                String collection = _requestExecutor.getConventions().getCollectionName(entity);
                if (collection != null) {
                    metadata.put(Constants.Documents.Metadata.COLLECTION, collection);
                }
            }

            if (!metadata.containsKey(Constants.Documents.Metadata.RAVEN_JAVA_TYPE)) {
                String javaType = _requestExecutor.getConventions().getJavaClassName(entity.getClass());
                if (javaType != null) {
                    metadata.put(Constants.Documents.Metadata.RAVEN_JAVA_TYPE, javaType);
                }
            }

            endPreviousCommandIfNeeded();

            try {
                if (!_first) {
                    writeComma();
                }

                _first = false;
                _inProgressCommand = CommandType.NONE;

                _currentWriter.write("{\"Id\":\"");
                writeString(id);
                _currentWriter.write("\",\"Type\":\"PUT\",\"Document\":");

                flushIfNeeded();

                DocumentInfo documentInfo = new DocumentInfo();
                documentInfo.setMetadataInstance(metadata);
                ObjectNode json = EntityToJson.convertEntityToJson(entity, _conventions, documentInfo, true);

                try (JsonGenerator generator =
                        objectMapper.getFactory().createGenerator(_currentWriter)) {
                    generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

                    generator.writeTree(json);
                }

                _currentWriter.write("}");
                flushIfNeeded();
            } catch (Exception e) {
                handleErrors(id, e);
            }
        } finally {
            _concurrentCheck.set(0);
        }
    }

    private void handleErrors(String documentId, Exception e) {
        BulkInsertAbortedException error = getExceptionFromOperation();
        if (error != null) {
            throw error;
        }

        throwOnUnavailableStream(documentId, e);
    }

    private CleanCloseable concurrencyCheck() {
        if (!_concurrentCheck.compareAndSet(0, 1)) {
            throw new IllegalStateException("Bulk Insert store methods cannot be executed concurrently.");
        }

        return () -> _concurrentCheck.compareAndSet(1, 0);
    }

    private void flushIfNeeded() throws IOException, ExecutionException, InterruptedException {
        _currentWriter.flush();

        if (_currentWriterBacking.size() > _maxSizeInBuffer || _asyncWrite.isDone()) {

            _asyncWrite.get();

            Writer tmp = _currentWriter;
            _currentWriter = _backgroundWriter;
            _backgroundWriter = tmp;

            ByteArrayOutputStream tmpBaos = _currentWriterBacking;
            _currentWriterBacking = _backgroundWriterBacking;
            _backgroundWriterBacking = tmpBaos;

            _currentWriterBacking.reset();

            final byte[] buffer = _backgroundWriterBacking.toByteArray();
            _asyncWrite = writeToRequestBodyStream(buffer);
        }
    }

    private CompletableFuture<Void> writeToRequestBodyStream(byte[] buffer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                _requestBodyStream.write(buffer);

                if (_isInitialWrite) {
                    _isInitialWrite = false;

                    // send this chunk
                    _requestBodyStream.flush();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, _executorService);
    }

    private void endPreviousCommandIfNeeded() {
        if (_inProgressCommand == CommandType.COUNTERS) {
            _countersOperation.endPreviousCommandIfNeeded();
        } else if (_inProgressCommand == CommandType.TIME_SERIES) {
            TimeSeriesBulkInsert.throwAlreadyRunningTimeSeries();
        }
    }

    private void writeString(String input) throws IOException {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ('"' == c) {
                if (i == 0 || input.charAt(i - 1) != '\\') {
                    _currentWriter.write("\\");
                }
            }
            _currentWriter.write(c);
        }
    }

    private void writeComma() throws IOException {
        _currentWriter.write(",");
    }

    private void executeBeforeStore() {
        if (_stream == null) {
            waitForId();
            ensureStream();
        }

        if (_bulkInsertExecuteTask.isCompletedExceptionally()) {
            try {
                _bulkInsertExecuteTask.get();
            } catch (ExecutionException | InterruptedException e) {
                throwBulkInsertAborted(e, null);
            }
        }
    }

    private static void verifyValidId(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalStateException("Document id must have a non empty value");
        }

        if (id.endsWith("|")) {
            throw new UnsupportedOperationException("Document ids cannot end with '|', but was called with " + id);
        }
    }

    private BulkInsertAbortedException getExceptionFromOperation() {
        GetOperationStateOperation.GetOperationStateCommand stateRequest =
                new GetOperationStateOperation.GetOperationStateCommand(_operationId, _nodeTag);
        _requestExecutor.execute(stateRequest);

        if (!"Faulted".equals(stateRequest.getResult().get("Status").asText())) {
            return null;
        }

        JsonNode result = stateRequest.getResult().get("Result");

        if (result.get("$type").asText().startsWith("Raven.Client.Documents.Operations.OperationExceptionResult")) {
            return new BulkInsertAbortedException(result.get("Error").asText());
        }

        return null;
    }

    private OutputStream _requestBodyStream;
    private ByteArrayOutputStream _currentWriterBacking;
    private Writer _currentWriter;
    private ByteArrayOutputStream _backgroundWriterBacking;
    private Writer _backgroundWriter;
    private CompletableFuture<Void> _asyncWrite = CompletableFuture.completedFuture(null);
    @SuppressWarnings("FieldCanBeLocal")
    private final int _maxSizeInBuffer = 1024 * 1024;

    private void ensureStream() {
        try {
            BulkInsertCommand bulkCommand = new BulkInsertCommand(_operationId, _streamExposerContent, _nodeTag, _options.isSkipOverwriteIfUnchanged());
            bulkCommand.useCompression = useCompression;

            _bulkInsertExecuteTask = CompletableFuture.supplyAsync(() -> {
                _requestExecutor.execute(bulkCommand);
                return null;
            }, _executorService);

            _stream = _streamExposerContent.outputStream.get();

            _requestBodyStream = _stream;

            _currentWriter.write('[');
        } catch (Exception e) {
            throw new RavenException("Unable to open bulk insert stream ", e);
        }
    }

    private void throwOnUnavailableStream(String id, Exception innerEx) {
        _streamExposerContent.errorOnProcessingRequest(new BulkInsertAbortedException("Write to stream failed at document with id " + id, innerEx));

        try {
            _bulkInsertExecuteTask.get();
        } catch (Exception e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }

    public void abort() {
        if (_operationId == -1) {
            return; // nothing was done, nothing to kill
        }

        waitForId();

        try {
            _requestExecutor.execute(new KillOperationCommand(_operationId, _nodeTag));
        } catch (RavenException e) {
            throw new BulkInsertAbortedException("Unable to kill ths bulk insert operation, because it was not found on the server.");
        }
    }

    @Override
    public void close() {
        endPreviousCommandIfNeeded();

        Exception flushEx = null;

        if (this._streamExposerContent.isDone()) {
            return;
        }

        if (_stream != null) {
            try {
                _currentWriter.write("]");
                _currentWriter.flush();

                _asyncWrite.get();

                byte[] buffer = _currentWriterBacking.toByteArray();
                _requestBodyStream.write(buffer);
                _stream.flush();
            } catch (Exception e) {
                flushEx = e;
            }
        }

        _streamExposerContent.done();

        if (_operationId == -1) {
            // closing without calling a single store.
            return;
        }

        if (_bulkInsertExecuteTask != null) {
            try {
                _bulkInsertExecuteTask.get();
            } catch (Exception e) {
                throwBulkInsertAborted(e, flushEx);
            }
        }

        if (_unsubscribeChanges != null) {
            _unsubscribeChanges.close();
        }
    }

    private final DocumentConventions _conventions;

    private final IDocumentStore _store;

    private String getId(Object entity) {
        Reference<String> idRef = new Reference<>();
        if (_generateEntityIdOnTheClient.tryGetIdFromInstance(entity, idRef)) {
            return idRef.value;
        }

        idRef.value = _generateEntityIdOnTheClient.generateDocumentKeyForStorage(entity);

        _generateEntityIdOnTheClient.trySetIdentity(entity, idRef.value); // set id property if it was null
        return idRef.value;
    }

    public AttachmentsBulkInsert attachmentsFor(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Document id cannot be null or empty");
        }

        return new AttachmentsBulkInsert(this, id);
    }

    public <TValues> TypedTimeSeriesBulkInsert<TValues> timeSeriesFor(Class<TValues> clazz, String id) {
        return timeSeriesFor(clazz, id, null);
    }

    public <TValues> TypedTimeSeriesBulkInsert<TValues> timeSeriesFor(Class<TValues> clazz, String id, String name) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Document id cannot be null or empty");
        }

        String tsName = name;
        if (tsName == null) {
            tsName = TimeSeriesOperations.getTimeSeriesName(clazz, _conventions);
        }

        if (StringUtils.isEmpty(tsName)) {
            throw new IllegalArgumentException("Time series name cannot be null or empty");
        }

        return new TypedTimeSeriesBulkInsert<>(this, clazz, id, tsName);
    }

    public CountersBulkInsert countersFor(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Document id cannot be null or empty");
        }

        return new CountersBulkInsert(this, id);
    }

    public TimeSeriesBulkInsert timeSeriesFor(String id, String name) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Document id cannot be null or empty");
        }

        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Time series name cannot be null or empty");
        }

        return new TimeSeriesBulkInsert(this, id, name);
    }

    public static class CountersBulkInsert {
        private final BulkInsertOperation _operation;
        private final String _id;

        public CountersBulkInsert(BulkInsertOperation operation, String id) {
            _operation = operation;
            _id = id;
        }

        public void increment(String name) {
            increment(name, 1L);
        }

        public void increment(String name, long delta) {
            _operation._countersOperation.increment(_id, name, delta);
        }
    }

    private static class CountersBulkInsertOperation {
        private final BulkInsertOperation _operation;
        private String _id;
        private boolean _first = true;
        private static final int MAX_COUNTERS_IN_BATCH = 1024;
        private int _countersInBatch = 0;

        public CountersBulkInsertOperation(BulkInsertOperation bulkInsertOperation) {
            _operation = bulkInsertOperation;
        }

        public void increment(String id, String name) {
            increment(id, name, 1L);
        }

        public void increment(String id, String name, long delta) {
            try (CleanCloseable check = _operation.concurrencyCheck()) {
                _operation.executeBeforeStore();

                if (_operation._inProgressCommand == CommandType.TIME_SERIES) {
                    TimeSeriesBulkInsert.throwAlreadyRunningTimeSeries();
                }


                try {

                    boolean isFirst = _id == null;

                    if (isFirst || !_id.equalsIgnoreCase(id)) {
                        if (!isFirst) {
                            //we need to end the command for the previous document id
                            _operation._currentWriter.write("]}},");
                        } else if (!_operation._first) {
                            _operation.writeComma();
                        }

                        _operation._first = false;

                        _id = id;
                        _operation._inProgressCommand = CommandType.COUNTERS;

                        writePrefixForNewCommand();
                    }

                    if (_countersInBatch >= MAX_COUNTERS_IN_BATCH) {
                        _operation._currentWriter.write("]}},");

                        writePrefixForNewCommand();
                    }

                    _countersInBatch++;

                    if (!_first) {
                        _operation.writeComma();
                    }

                    _first = false;

                    _operation._currentWriter.write("{\"Type\":\"Increment\",\"CounterName\":\"");
                    _operation.writeString(name);
                    _operation._currentWriter.write("\",\"Delta\":");
                    _operation._currentWriter.write(String.valueOf(delta));
                    _operation._currentWriter.write("}");

                    _operation.flushIfNeeded();
                } catch (Exception e) {
                    _operation.handleErrors(_id, e);
                }
            }
        }

        public void endPreviousCommandIfNeeded() {
            if (_id == null) {
                return;
            }

            try {
                _operation._currentWriter.write("]}}");
                _id = null;
            } catch (IOException e) {
                throw new RavenException("Unable to write to stream", e);
            }
        }

        private void writePrefixForNewCommand() throws IOException {
            _first = true;
            _countersInBatch = 0;

            _operation._currentWriter.write("{\"Id\":\"");
            _operation.writeString(_id);
            _operation._currentWriter.write("\",\"Type\":\"Counters\",\"Counters\":{\"DocumentId\":\"");
            _operation.writeString(_id);
            _operation._currentWriter.write("\",\"Operations\":[");
        }
    }

    public static abstract class TimeSeriesBulkInsertBase implements Closeable {
        private final BulkInsertOperation _operation;
        private final String _id;
        private final String _name;
        private boolean _first = true;
        private int _timeSeriesInBatch = 0;

        protected TimeSeriesBulkInsertBase(BulkInsertOperation operation, String id, String name) {
            operation.endPreviousCommandIfNeeded();

            _operation = operation;
            _id = id;
            _name = name;

            _operation._inProgressCommand = CommandType.TIME_SERIES;
        }

        protected void appendInternal(Date timestamp, Collection<Double> values, String tag) {
            try (CleanCloseable check = _operation.concurrencyCheck()) {
                _operation.executeBeforeStore();

                try {
                    if (_first) {
                        if (!_operation._first) {
                            _operation.writeComma();
                        }

                        writePrefixForNewCommand();
                    } else if (_timeSeriesInBatch >= _operation._timeSeriesBatchSize) {
                        _operation._currentWriter.write("]}},");
                        writePrefixForNewCommand();
                    }

                    _timeSeriesInBatch++;

                    if (!_first) {
                        _operation.writeComma();
                    }

                    _first = false;

                    _operation._currentWriter.write("[");

                    _operation._currentWriter.write(String.valueOf(timestamp.getTime()));
                    _operation.writeComma();

                    _operation._currentWriter.write(String.valueOf(values.size()));
                    _operation.writeComma();

                    boolean firstValue = true;

                    for (Double value : values) {
                        if (!firstValue) {
                            _operation.writeComma();
                        }

                        firstValue = false;
                        _operation._currentWriter.write(String.valueOf(value));
                    }

                    if (tag != null) {
                        _operation._currentWriter.write(",\"");
                        _operation.writeString(tag);
                        _operation._currentWriter.write("\"");
                    }

                    _operation._currentWriter.write("]");

                    _operation.flushIfNeeded();
                } catch (Exception e) {
                    _operation.handleErrors(_id, e);
                }
            }
        }

        private void writePrefixForNewCommand() throws IOException {
            _first = true;
            _timeSeriesInBatch = 0;

            _operation._currentWriter.write("{\"Id\":\"");
            _operation.writeString(_id);
            _operation._currentWriter.write("\",\"Type\":\"TimeSeriesBulkInsert\",\"TimeSeries\":{\"Name\":\"");
            _operation.writeString(_name);
            _operation._currentWriter.write("\",\"TimeFormat\":\"UnixTimeInMs\",\"Appends\":[");
        }

        static void throwAlreadyRunningTimeSeries() {
            throw new IllegalStateException("There is an already running time series operation, did you forget to close it?");
        }

        @Override
        public void close() throws IOException {
            _operation._inProgressCommand = CommandType.NONE;

            if (!_first) {
                _operation._currentWriter.write("]}}");
            }
        }
    }

    public static class TimeSeriesBulkInsert extends TimeSeriesBulkInsertBase {
        public TimeSeriesBulkInsert(BulkInsertOperation operation, String id, String name) {
            super(operation, id, name);
        }

        public void append(Date timestamp, double value) {
            append(timestamp, value, null);
        }

        public void append(Date timestamp, double value, String tag) {
            appendInternal(timestamp, Collections.singletonList(value), tag);
        }

        public void append(Date timestamp, double[] values) {
            append(timestamp, values, null);
        }

        public void append(Date timestamp, double[] values, String tag) {
            appendInternal(timestamp, DoubleStream.of(values).boxed().collect(Collectors.toList()), tag);
        }

        public void append(Date timestamp, Collection<Double> values) {
            append(timestamp, values, null);
        }

        public void append(Date timestamp, Collection<Double> values, String tag) {
            appendInternal(timestamp, values, tag);
        }
    }

    public static class TypedTimeSeriesBulkInsert<T> extends TimeSeriesBulkInsertBase {

        private final Class<T> clazz;

        public TypedTimeSeriesBulkInsert(BulkInsertOperation operation, Class<T> clazz, String id, String name) {
            super(operation, id, name);

            this.clazz = clazz;
        }

        public void append(Date timestamp, T value) {
            append(timestamp, value, null);
        }

        public void append(Date timestamp, T value, String tag) {
            double[] values = TimeSeriesValuesHelper.getValues(clazz, value);
            appendInternal(timestamp, DoubleStream.of(values).boxed().collect(Collectors.toList()), tag);
        }

        public void append(TypedTimeSeriesEntry<T> entry) {
            append(entry.getTimestamp(), entry.getValue(), entry.getTag());
        }
    }

    public static class AttachmentsBulkInsert {
        private final BulkInsertOperation _operation;
        private final String _id;

        public AttachmentsBulkInsert(BulkInsertOperation operation, String id) {
            _operation = operation;
            _id = id;
        }

        public void store(String name, byte[] bytes) {
            store(name, bytes, null);
        }

        public void store(String name, byte[] bytes, String contentType) {
            _operation._attachmentsOperation.store(_id, name, bytes, contentType);
        }
    }

    private static class AttachmentsBulkInsertOperation {
        private final BulkInsertOperation _operation;

        public AttachmentsBulkInsertOperation(BulkInsertOperation operation) {
            _operation = operation;
        }

        public void store(String id, String name, byte[] bytes) {
            store(id, name, bytes, null);
        }

        public void store(String id, String name, byte[] bytes, String contentType) {
            try (CleanCloseable check = _operation.concurrencyCheck()) {
                _operation.endPreviousCommandIfNeeded();

                _operation.executeBeforeStore();

                try {
                    if (!_operation._first) {
                        _operation.writeComma();
                    }

                    _operation._currentWriter.write("{\"Id\":\"");
                    _operation.writeString(id);
                    _operation._currentWriter.write("\",\"Type\":\"AttachmentPUT\",\"Name\":\"");
                    _operation.writeString(name);

                    if (contentType != null) {
                        _operation._currentWriter.write("\",\"ContentType\":\"");
                        _operation.writeString(contentType);
                    }

                    _operation._currentWriter.write("\",\"ContentLength\":");
                    _operation._currentWriter.write(String.valueOf(bytes.length));
                    _operation._currentWriter.write("}");

                    _operation.flushIfNeeded();

                    _operation._currentWriter.flush();
                    _operation._currentWriterBacking.write(bytes);

                    _operation._currentWriterBacking.flush();

                    _operation.flushIfNeeded();
                } catch (Exception e) {
                    _operation.handleErrors(id, e);
                }
            }
        }
    }
}
