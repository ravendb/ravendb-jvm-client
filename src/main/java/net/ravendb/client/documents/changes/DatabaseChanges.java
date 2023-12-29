package net.ravendb.client.documents.changes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.*;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.*;

@SuppressWarnings("UnnecessaryLocalVariable")
public class DatabaseChanges extends AbstractDatabaseChanges<DatabaseConnectionState> implements IDatabaseChanges {

    public DatabaseChanges(RequestExecutor requestExecutor, String databaseName, ExecutorService executorService, Runnable onDispose, String nodeTag) {
        super(requestExecutor, databaseName, executorService, onDispose, nodeTag);
    }

    @Override
    public DatabaseChanges ensureConnectedNow() {
        return (DatabaseChanges)super.ensureConnectedNow();
    }

    @Override
    public IChangesObservable<DocumentChange> forDocument(String docId) {
        if (StringUtils.isBlank(docId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace");
        }
        DatabaseConnectionState counter = getOrAddConnectionState("docs/" + docId, "watch-doc", "unwatch-doc", docId);

        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> StringUtils.equalsIgnoreCase(notification.getId(), docId));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forAllDocuments() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-docs", "watch-docs", "unwatch-docs", null);
        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> true);

        return taskedObservable;
    }

    public IChangesObservable<AggressiveCacheChange> forAggressiveCaching() {
        DatabaseConnectionState counter = getOrAddConnectionState("aggressive-caching", "watch-aggressive-caching", "unwatch-aggressive-caching", null);

        ChangesObservable<AggressiveCacheChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<AggressiveCacheChange, DatabaseConnectionState>(ChangesType.AGGRESSIVE_CACHE, counter, notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<OperationStatusChange> forOperationId(long operationId) {
        DatabaseConnectionState counter = getOrAddConnectionState("operations/" + operationId, "watch-operation", "unwatch-operation", String.valueOf(operationId));

        ChangesObservable<OperationStatusChange, DatabaseConnectionState> taskedObservable
                = new ChangesObservable<>(ChangesType.OPERATION, counter, notification -> notification.getOperationId() == operationId);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<OperationStatusChange> forAllOperations() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-operations", "watch-operations", "unwatch-operations", null);

        ChangesObservable<OperationStatusChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.OPERATION, counter,
                notification -> true);

        return taskedObservable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IChangesObservable<IndexChange> forIndex(String indexName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("IndexName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("indexes/" + indexName, "watch-index", "unwatch-index", indexName);

        ChangesObservable taskedObservable = new ChangesObservable<IndexChange, DatabaseConnectionState>(
                ChangesType.INDEX, counter, notification -> StringUtils.equalsIgnoreCase(notification.getName(), indexName));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<IndexChange> forAllIndexes() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-indexes", "watch-indexes", "unwatch-indexes", null);

        ChangesObservable<IndexChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.INDEX, counter, notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsStartingWith(String docIdPrefix) {
        if (StringUtils.isBlank(docIdPrefix)) {
            throw new IllegalArgumentException("DocumentIdPrefix cannot be null or whitespace");
        }
        DatabaseConnectionState counter = getOrAddConnectionState("prefixes/" + docIdPrefix, "watch-prefix", "unwatch-prefix", docIdPrefix);
        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> notification.getId() != null && StringUtils.startsWithIgnoreCase(notification.getId(), docIdPrefix));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsInCollection(String collectionName) {
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalArgumentException("CollectionName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("collections/" + collectionName, "watch-collection", "unwatch-collection", collectionName);

        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> StringUtils.equalsIgnoreCase(collectionName, notification.getCollectionName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsInCollection(Class<?> clazz) {
        String collectionName = _conventions.getCollectionName(clazz);
        return forDocumentsInCollection(collectionName);
    }

    @Override
    public IChangesObservable<CounterChange> forAllCounters() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-counters", "watch-counters", "unwatch-counters", null);

        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCounter(String counterName) {
        if (StringUtils.isBlank(counterName)) {
            throw new IllegalArgumentException("CounterName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("counter/" + counterName, "watch-counter", "unwatch-counter", counterName);
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(counterName, notification.getName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCounterOfDocument(String documentId, String counterName) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(counterName)) {
            throw new IllegalArgumentException("CounterName cannot be null or whitespace.");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/counter/" + counterName, "watch-document-counter", "unwatch-document-counter", null, new String[]{documentId, counterName});
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId()) && StringUtils.equalsIgnoreCase(counterName, notification.getName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCountersOfDocument(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/counter", "watch-document-counters", "unwatch-document-counters", documentId);
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<TimeSeriesChange> forAllTimeSeries() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-timeseries",
                "watch-all-timeseries", "unwatch-all-timeseries", null);

        ChangesObservable<TimeSeriesChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(
                ChangesType.TIME_SERIES, counter, notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<TimeSeriesChange> forTimeSeries(String timeSeriesName) {
        if (StringUtils.isBlank(timeSeriesName)) {
            throw new IllegalArgumentException("TimeSeriesName cannot be null or whitespace.");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("timeseries/" + timeSeriesName,
                "watch-timeseries", "unwatch-timeseries", timeSeriesName);

        ChangesObservable<TimeSeriesChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.TIME_SERIES, counter,
                notification -> StringUtils.equalsIgnoreCase(timeSeriesName, notification.getName()));

        return taskedObservable;
    }



    @Override
    public IChangesObservable<TimeSeriesChange> forTimeSeriesOfDocument(String documentId, String timeSeriesName) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(timeSeriesName)) {
            throw new IllegalArgumentException("TimeSeriesName cannot be null or whitespace.");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/timeseries/" + timeSeriesName,
                "watch-document-timeseries", "unwatch-document-timeseries", null, new String[]{documentId, timeSeriesName});

        ChangesObservable<TimeSeriesChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.TIME_SERIES, counter,
                notification -> StringUtils.equalsIgnoreCase(timeSeriesName, notification.getName()) && StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<TimeSeriesChange> forTimeSeriesOfDocument(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace.");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/timeseries",
                "watch-all-document-timeseries", "unwatch-all-document-timeseries", documentId);

        ChangesObservable<TimeSeriesChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(
                ChangesType.TIME_SERIES, counter, notification -> StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId())
        );

        return taskedObservable;
    }

    @Override
    protected void processNotification(String type, ObjectNode change) throws JsonProcessingException {
        ObjectNode value = (ObjectNode) change.get("Value");
        notifySubscribers(type, value);
    }

    protected void notifySubscribers(String type, ObjectNode value) throws JsonProcessingException {
        switch (type) {
            case "AggressiveCacheChange":
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(AggressiveCacheChange.INSTANCE);
                }
                break;
            case "DocumentChange":
                DocumentChange documentChange = JsonExtensions.getDefaultMapper().treeToValue(value, DocumentChange.class);
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(documentChange);
                }
                break;
            case "CounterChange":
                CounterChange counterChange = JsonExtensions.getDefaultMapper().treeToValue(value, CounterChange.class);
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(counterChange);
                }
                break;
            case "TimeSeriesChange":
                TimeSeriesChange timeSeriesChange = JsonExtensions.getDefaultMapper().treeToValue(value, TimeSeriesChange.class);
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(timeSeriesChange);
                }
                break;
            case "IndexChange":
                IndexChange indexChange = JsonExtensions.getDefaultMapper().treeToValue(value, IndexChange.class);
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(indexChange);
                }
                break;
            case "OperationStatusChange":
                OperationStatusChange operationStatusChange = JsonExtensions.getDefaultMapper().treeToValue(value, OperationStatusChange.class);
                for (DatabaseConnectionState state : _states.values()) {
                    state.send(operationStatusChange);
                }
                break;

            case "TopologyChange":
                TopologyChange topologyChange = JsonExtensions.getDefaultMapper().treeToValue(value, TopologyChange.class);

                RequestExecutor requestExecutor = _requestExecutor;
                if (requestExecutor != null) {
                    ServerNode node = new ServerNode();
                    node.setUrl(topologyChange.getUrl());
                    node.setDatabase(topologyChange.getDatabase());

                    UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(node);
                    updateParameters.setTimeoutInMs(0);
                    updateParameters.setForceUpdate(true);
                    updateParameters.setDebugTag("topology-change-notification");

                    requestExecutor.updateTopologyAsync(updateParameters);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
