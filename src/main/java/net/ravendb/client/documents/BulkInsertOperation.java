package net.ravendb.client.documents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.commands.KillOperationCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.operations.GetOperationStateOperation;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.documents.bulkinsert.BulkInsertAbortedException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.primitives.Reference;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class BulkInsertOperation implements CleanCloseable {

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

        private final long _id;

        private final boolean useCompression;

        public BulkInsertCommand(long id, StreamExposerContent stream, boolean useCompression) {
            super(CloseableHttpResponse.class);
            _stream = stream;
            _id = id;
            this.useCompression = useCompression;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/bulk_insert?id=" + _id;

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
    }

    private ExecutorService _executorService;
    private final RequestExecutor _requestExecutor;
    private CompletableFuture<Void> _bulkInsertExecuteTask;
    private final ObjectMapper objectMapper;

    private OutputStream _stream;
    private final StreamExposerContent _streamExposerContent;

    private boolean _first = true;
    private long _operationId = -1;

    private boolean useCompression = false;

    private final AtomicInteger _concurrentCheck = new AtomicInteger();

    public BulkInsertOperation(String database, DocumentStore store) {
        _executorService = store.getExecutorService();
        _conventions = store.getConventions();
        _requestExecutor = store.getRequestExecutor(database);
        objectMapper = store.getConventions().getEntityMapper();

        _currentWriterBacking = new ByteArrayOutputStream();
        _currentWriter = new OutputStreamWriter(_currentWriterBacking);
        _backgroundWriterBacking = new ByteArrayOutputStream();
        _backgroundWriter = new OutputStreamWriter(_backgroundWriterBacking);
        _streamExposerContent = new StreamExposerContent();

        _generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(),
                entity -> _requestExecutor.getConventions().generateDocumentId(database, entity));
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    private void throwBulkInsertAborted(Exception e, Exception flushEx) {
        Exception error = getExceptionFromOperation();

        throw new BulkInsertAbortedException("Failed to execute bulk insert", ObjectUtils.firstNonNull(error, e, flushEx));
    }

    private void waitForId() {
        if (_operationId != -1) {
            return;
        }

        GetNextOperationIdCommand bulkInsertGetIdRequest = new GetNextOperationIdCommand();
        _requestExecutor.execute(bulkInsertGetIdRequest);
        _operationId = bulkInsertGetIdRequest.getResult();
    }

    public void store(Object entity, String id)  {
        store(entity, id, null);
    }

    public void store(Object entity, String id, IMetadataDictionary metadata) {
        if (!_concurrentCheck.compareAndSet(0, 1)) {
            throw new IllegalStateException("Bulk Insert store methods cannot be executed concurrently.");
        }

        try {
            verifyValidId(id);

            if (_stream == null) {
                waitForId();
                ensureStream();
            }

            if (_bulkInsertExecuteTask.isCompletedExceptionally()) {
                try {
                    _bulkInsertExecuteTask.get();
                } catch (Exception e) {
                    throwBulkInsertAborted(e, null);
                }
            }

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

            try {
                if (!_first) {
                    _currentWriter.write(",");
                }

                _first = false;

                _currentWriter.write("{\"Id\":\"");
                writeId(_currentWriter, id);
                _currentWriter.write("\",\"Type\":\"PUT\",\"Document\":");

                DocumentInfo documentInfo = new DocumentInfo();
                documentInfo.setMetadataInstance(metadata);
                ObjectNode json = EntityToJson.convertEntityToJson(entity, _conventions, documentInfo, false);

                _currentWriter.flush();

                try (JsonGenerator generator =
                        objectMapper.getFactory().createGenerator(_currentWriter)) {
                    generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

                    generator.writeTree(json);
                }

                _currentWriter.write("}");
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
                    _asyncWrite = CompletableFuture.supplyAsync(() -> {
                        try {
                            _requestBodyStream.write(buffer);

                            // send this chunk
                            _requestBodyStream.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }, _executorService);
                }
            } catch (Exception e) {
                RuntimeException error = getExceptionFromOperation();
                if (error != null) {
                    throw error;
                }

                throwOnUnavailableStream(id, e);
            }
        } finally {
            _concurrentCheck.set(0);
        }
    }

    private void writeId(Writer writer, String input) throws IOException {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ('"' == c) {
                if (i == 0 || input.charAt(i - 1) != '\\') {
                    writer.write("\\");
                }
            }
            writer.write(c);
        }
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

    private static void verifyValidId(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalStateException("Document id must have a non empty value");
        }

        if (id.endsWith("|")) {
            throw new UnsupportedOperationException("Document ids cannot end with '|', but was called with " + id);
        }
    }

    private BulkInsertAbortedException getExceptionFromOperation() {
        GetOperationStateOperation.GetOperationStateCommand stateRequest = new GetOperationStateOperation.GetOperationStateCommand(_requestExecutor.getConventions(), _operationId);
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
            BulkInsertCommand bulkCommand = new BulkInsertCommand(_operationId, _streamExposerContent, useCompression);

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
            _requestExecutor.execute(new KillOperationCommand(_operationId));
        } catch (RavenException e) {
            throw new BulkInsertAbortedException("Unable to kill ths bulk insert operation, because it was not found on the server.");
        }
    }

    @Override
    public void close() {
        Exception flushEx = null;

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
    }

    private final DocumentConventions _conventions;

    private String getId(Object entity) {
        Reference<String> idRef = new Reference<>();
        if (_generateEntityIdOnTheClient.tryGetIdFromInstance(entity, idRef)) {
            return idRef.value;
        }

        idRef.value = _generateEntityIdOnTheClient.generateDocumentKeyForStorage(entity);

        _generateEntityIdOnTheClient.trySetIdentity(entity, idRef.value); // set id property if it was null
        return idRef.value;
    }

}
