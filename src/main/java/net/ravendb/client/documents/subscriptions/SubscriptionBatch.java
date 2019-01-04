package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.json.MetadataAsDictionary;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionBatch<T> {

    /**
     * Represents a single item in a subscription batch results. This class should be used only inside the subscription's Run delegate, using it outside this scope might cause unexpected behavior.
     */
    public static class Item<T> {
        private T _result;
        private String exceptionMessage;
        private String id;
        private String changeVector;
        private boolean projection;

        private void throwItemProcessException() {
            throw new IllegalStateException("Failed to process document " + id + " with Change Vector " + changeVector + " because: " + System.lineSeparator() + exceptionMessage);
        }

        public String getExceptionMessage() {
            return exceptionMessage;
        }

        public String getId() {
            return id;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public boolean isProjection() {
            return projection;
        }

        public T getResult() {
            if (exceptionMessage != null) {
                throwItemProcessException();
            }
            return _result;
        }

        void setResult(T result) {
            _result = result;
        }

        private ObjectNode rawResult;
        private ObjectNode rawMetadata;

        public ObjectNode getRawResult() {
            return rawResult;
        }

        public ObjectNode getRawMetadata() {
            return rawMetadata;
        }

        void setRawResult(ObjectNode rawResult) {
            this.rawResult = rawResult;
        }

        void setRawMetadata(ObjectNode rawMetadata) {
            this.rawMetadata = rawMetadata;
        }

        private IMetadataDictionary _metadata;

        public IMetadataDictionary getMetadata() {
            if (_metadata == null) {
                _metadata = new MetadataAsDictionary(rawMetadata);
            }

            return _metadata;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public int getNumberOfItemsInBatch() {
        return _items != null ? _items.size() : 0;
    }

    private final Class<T> _clazz;
    private final boolean _revisions;
    private final RequestExecutor _requestExecutor;
    private final IDocumentStore _store;
    private final String _dbName;
    private final Log _logger;
    private final GenerateEntityIdOnTheClient _generateEntityIdOnTheClient;

    private final List<Item<T>> _items = new ArrayList<>();
    private List<ObjectNode> _includes;

    public List<Item<T>> getItems() {
        return _items;
    }

    public IDocumentSession openSession() {
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setDatabase(_dbName);
        sessionOptions.setRequestExecutor(_requestExecutor);
        return openSessionInternal(sessionOptions);
    }

    public IDocumentSession openSession(SessionOptions options) {
        validateSessionOptions(options);

        options.setDatabase(_dbName);
        options.setRequestExecutor(_requestExecutor);

        return openSessionInternal(options);
    }

    private IDocumentSession openSessionInternal(SessionOptions options) {
        IDocumentSession s = _store.openSession(options);

        loadDataToSession((InMemoryDocumentSessionOperations) s);
        return s;
    }

    private static void validateSessionOptions(SessionOptions options) {
        if (options.getDatabase() != null) {
            throw new IllegalStateException("Cannot set Database when session is opened in subscription.");
        }

        if (options.getRequestExecutor() != null) {
            throw new IllegalStateException("Cannot set RequestExecutor when session is opened in subscription.");
        }

        if (options.getTransactionMode() != TransactionMode.SINGLE_NODE) {
            throw new IllegalStateException("Cannot set TransactionMode when session is opened in subscription. Only SINGLE_NODE is supported.");
        }
    }

    private void loadDataToSession(InMemoryDocumentSessionOperations s) {
        if (s.noTracking) {
            return;
        }

        if (_includes == null) {
            return;
        }

        for (ObjectNode item : _includes) {
            s.registerIncludes(item);
        }

        for (Item<T> item : getItems()) {
            if (item.projection) {
                continue;
            }

            DocumentInfo documentInfo = new DocumentInfo();
            documentInfo.setId(item.getId());
            documentInfo.setDocument(item.getRawResult());
            documentInfo.setMetadata(item.getRawMetadata());
            documentInfo.setChangeVector(item.getChangeVector());
            documentInfo.setEntity(item.getResult());
            documentInfo.setNewDocument(false);
            s.registerExternalLoadedIntoTheSession(documentInfo);
        }

    }

    public SubscriptionBatch(Class<T> clazz, boolean revisions, RequestExecutor requestExecutor, IDocumentStore store, String dbName, Log logger) {
        _clazz = clazz;
        _revisions = revisions;
        _requestExecutor = requestExecutor;
        _store = store;
        _dbName = dbName;
        _logger = logger;

        _generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(), entity -> { throw new IllegalStateException("Shouldn't be generating new ids here"); });
    }

    @SuppressWarnings("unchecked")
    String initialize(BatchFromServer batch) {
        _items.clear();
        String lastReceivedChangeVector = null;

        _includes = batch.getIncludes();

        for (SubscriptionConnectionServerMessage item : batch.getMessages()) {
            ObjectNode metadata;
            ObjectNode curDoc = item.getData();

            metadata = (ObjectNode) curDoc.get(Constants.Documents.Metadata.KEY);
            if (metadata == null) {
                throwRequired("@metadata field");
            }

            JsonNode idNode = metadata.get(Constants.Documents.Metadata.ID);
            if (idNode == null) {
                throwRequired("@id field");
            }
            String id = idNode.asText();

            String changeVector = null;

            JsonNode changeVectorNode = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR);
            if (changeVectorNode == null || changeVectorNode.asText() == null) {
                throwRequired("@change-vector field");
            } else {
                changeVector = lastReceivedChangeVector = changeVectorNode.asText();
            }

            boolean projection = false;

            JsonNode projectionNode = metadata.get(Constants.Documents.Metadata.PROJECTION);
            if (projectionNode != null && projectionNode.isBoolean()) {
                projection = projectionNode.asBoolean();
            }

            if (_logger.isDebugEnabled()) {
                _logger.debug("Got " + id + " (change vector: [" + lastReceivedChangeVector + "], size: " + curDoc.size() + ")");
            }

            T instance = null;

            if (item.getException() == null) {
                if (ObjectNode.class.equals(_clazz)) {
                    instance = (T) curDoc;
                } else {
                    if (_revisions) {
                        // parse outer object manually as Previous/Current has PascalCase
                        JsonNode previous = curDoc.get("Previous");
                        JsonNode current = curDoc.get("Current");
                        Revision<T> revision = new Revision<>();
                        if (current != null && !current.isNull()) {
                            revision.setCurrent((T) EntityToJson.convertToEntity(_clazz, id, (ObjectNode) current, _requestExecutor.getConventions()));
                        }
                        if (previous != null && !previous.isNull()) {
                            revision.setPrevious((T) EntityToJson.convertToEntity(_clazz, id, (ObjectNode) previous, _requestExecutor.getConventions()));
                        }
                        instance = (T) revision;
                    } else {
                        instance = (T) EntityToJson.convertToEntity(_clazz, id, curDoc, _requestExecutor.getConventions());
                    }
                }

                if (StringUtils.isNotEmpty(id)) {
                    _generateEntityIdOnTheClient.trySetIdentity(instance, id);
                }
            }

            Item itemToAdd = new Item();
            itemToAdd.changeVector = changeVector;
            itemToAdd.id = id;
            itemToAdd.rawResult = curDoc;
            itemToAdd.rawMetadata = metadata;
            itemToAdd._result = instance;
            itemToAdd.exceptionMessage = item.getException();
            itemToAdd.projection = projection;

            _items.add(itemToAdd);
        }

        return lastReceivedChangeVector;
    }

    private static void throwRequired(String name) {
        throw new IllegalStateException("Document must have a " + name);
    }
}
