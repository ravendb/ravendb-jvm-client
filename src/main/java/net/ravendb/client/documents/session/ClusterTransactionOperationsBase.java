package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.operations.compareExchange.*;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public abstract class ClusterTransactionOperationsBase {
    protected final DocumentSession session;
    private final Map<String, CompareExchangeSessionValue> _state = new TreeMap<>(String::compareToIgnoreCase);

    private Map<String, String> _missingDocumentsTooAtomicGuardIndex;

    protected boolean tryGetMissingAtomicGuardFor(String docId, Reference<String> changeVector) {
        if (_missingDocumentsTooAtomicGuardIndex == null) {
            changeVector.value = null;
            return false;
        }

        changeVector.value = _missingDocumentsTooAtomicGuardIndex.get(docId);
        return changeVector.value != null;
    }

    public int getNumberOfTrackedCompareExchangeValues() {
        return _state.size();
    }

    public ClusterTransactionOperationsBase(DocumentSession session) {
        if (session.getTransactionMode() != TransactionMode.CLUSTER_WIDE) {
            throw new IllegalStateException("This function is part of cluster transaction session, in order to use it you have to open the Session with ClusterWide option.");
        }

        this.session = session;
    }

    public DocumentSession getSession() {
        return session;
    }

    public boolean isTracked(String key) {
        Reference<CompareExchangeSessionValue> ref = new Reference<>();
        return tryGetCompareExchangeValueFromSession(key, ref);
    }

    public <T> CompareExchangeValue<T> createCompareExchangeValue(String key, T item) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        Reference<CompareExchangeSessionValue> sessionValueRef = new Reference<>();
        if (!tryGetCompareExchangeValueFromSession(key, sessionValueRef)) {
            sessionValueRef.value = new CompareExchangeSessionValue(key, 0, CompareExchangeValueState.NONE);
            _state.put(key, sessionValueRef.value);
        }

        return sessionValueRef.value.create(item);
    }

    public <T> void deleteCompareExchangeValue(CompareExchangeValue<T> item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        Reference<CompareExchangeSessionValue> sessionValueRef = new Reference<>();
        if (!tryGetCompareExchangeValueFromSession(item.getKey(), sessionValueRef)) {
            sessionValueRef.value = new CompareExchangeSessionValue(item.getKey(), 0, CompareExchangeValueState.NONE);
            _state.put(item.getKey(), sessionValueRef.value);
        }

        sessionValueRef.value.delete(item.getIndex());
    }

    public void deleteCompareExchangeValue(String key, long index) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        Reference<CompareExchangeSessionValue> sessionValueRef = new Reference<>();
        if (!tryGetCompareExchangeValueFromSession(key, sessionValueRef)) {
            sessionValueRef.value = new CompareExchangeSessionValue(key, 0, CompareExchangeValueState.NONE);
            _state.put(key, sessionValueRef.value);
        }

        sessionValueRef.value.delete(index);
    }

    public void clear() {
        _state.clear();
    }

    protected <T> CompareExchangeValue<T> getCompareExchangeValueInternal(Class<T> clazz, String key) {
        Reference<Boolean> notTrackedReference = new Reference<>();
        CompareExchangeValue<T> v = getCompareExchangeValueFromSessionInternal(clazz, key, notTrackedReference);
        if (!notTrackedReference.value) {
            return v;
        }

        session.incrementRequestCount();

        CompareExchangeValue<ObjectNode> value = session.getOperations().send(
                new GetCompareExchangeValueOperation<>(ObjectNode.class, key, false), session.sessionInfo);
        if (value == null) {
            registerMissingCompareExchangeValue(key);
            return null;
        }

        CompareExchangeSessionValue sessionValue = registerCompareExchangeValue(value);
        if (sessionValue != null) {
            return sessionValue.getValue(clazz, session.getConventions());
        }

        return null;
    }

    protected <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValuesInternal(Class<T> clazz, String[] keys) {
        Reference<Set<String>> notTrackedKeys = new Reference<>();
        Map<String, CompareExchangeValue<T>> results = getCompareExchangeValuesFromSessionInternal(clazz, keys, notTrackedKeys);

        if (notTrackedKeys.value == null || notTrackedKeys.value.isEmpty()) {
            return results;
        }

        session.incrementRequestCount();

        String[] keysArray = notTrackedKeys.value.toArray(new String[0]);
        Map<String, CompareExchangeValue<ObjectNode>> values = session.getOperations().send(new GetCompareExchangeValuesOperation<>(ObjectNode.class, keysArray), session.sessionInfo);

        for (String key : keysArray) {
            CompareExchangeValue<ObjectNode> value = values.get(key);
            if (value == null) {
                registerMissingCompareExchangeValue(key);
                results.put(key, null);
                continue;
            }

            CompareExchangeSessionValue sessionValue = registerCompareExchangeValue(value);
            results.put(value.getKey(), sessionValue.getValue(clazz, session.getConventions()));
        }

        return results;
    }

    protected <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValuesInternal(Class<T> clazz, String startsWith, int start, int pageSize) {
        session.incrementRequestCount();

        Map<String, CompareExchangeValue<ObjectNode>> values = session.getOperations().send(
                new GetCompareExchangeValuesOperation<ObjectNode>(ObjectNode.class, startsWith, start, pageSize), session.getSessionInfo());

        Map<String, CompareExchangeValue<T>> results = new HashMap<>();

        for (Map.Entry<String, CompareExchangeValue<ObjectNode>> keyValue : values.entrySet()) {

            String key = keyValue.getKey();
            CompareExchangeValue<ObjectNode> value = keyValue.getValue();

            if (value == null) {
                registerMissingCompareExchangeValue(key);
                results.put(key, null);
                continue;
            }

            CompareExchangeSessionValue sessionValue = registerCompareExchangeValue(value);
            results.put(key, sessionValue.getValue(clazz, session.getConventions()));
        }

        return results;
    }

    public <T> CompareExchangeValue<T> getCompareExchangeValueFromSessionInternal(Class<T> clazz, String key, Reference<Boolean> notTracked) {
        Reference<CompareExchangeSessionValue> sessionValueReference = new Reference<>();
        if (tryGetCompareExchangeValueFromSession(key, sessionValueReference)) {
            notTracked.value = false;
            return sessionValueReference.value.getValue(clazz, session.getConventions());
        }

        notTracked.value = true;
        return null;
    }

    public <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValuesFromSessionInternal(Class<T> clazz, String[] keys, Reference<Set<String>> notTrackedKeys) {
        notTrackedKeys.value = null;

        Map<String, CompareExchangeValue<T>> results = new TreeMap<>(String::compareToIgnoreCase);

        if (keys == null || keys.length == 0) {
            return results;
        }

        for (String key : keys) {
            Reference<CompareExchangeSessionValue> sessionValueRef = new Reference<>();
            if (tryGetCompareExchangeValueFromSession(key, sessionValueRef)) {
                results.put(key, sessionValueRef.value.getValue(clazz, session.getConventions()));
                continue;
            }

            if (notTrackedKeys.value == null) {
                notTrackedKeys.value = new TreeSet<>(String::compareToIgnoreCase);
            }

            notTrackedKeys.value.add(key);
        }

        return results;
    }

    public CompareExchangeSessionValue registerMissingCompareExchangeValue(String key) {
        CompareExchangeSessionValue value = new CompareExchangeSessionValue(key, -1, CompareExchangeValueState.MISSING);
        if (session.noTracking) {
            return value;
        }

        _state.put(key, value);
        return value;
    }

    public void registerCompareExchangeValues(ObjectNode values, boolean includingMissingAtomicGuards) {
        if (session.noTracking) {
            return;
        }

        if (values != null) {
            Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> propertyDetails = fields.next();

                CompareExchangeValue<ObjectNode> val = CompareExchangeValueResultParser.getSingleValue(
                        ObjectNode.class, (ObjectNode) propertyDetails.getValue(), false, session.getConventions());

                if (includingMissingAtomicGuards
                        && StringUtils.startsWithIgnoreCase(val.getKey(), Constants.CompareExchange.RVN_ATOMIC_PREFIX)
                        && val.getChangeVector() != null) {
                    if (_missingDocumentsTooAtomicGuardIndex == null) {
                        _missingDocumentsTooAtomicGuardIndex = new HashMap<>();
                    }

                    _missingDocumentsTooAtomicGuardIndex.put(val.getKey().substring(Constants.CompareExchange.RVN_ATOMIC_PREFIX.length()), val.getChangeVector());
                } else {
                    registerCompareExchangeValue(val);
                }
            }
        }
    }

    public CompareExchangeSessionValue registerCompareExchangeValue(CompareExchangeValue<ObjectNode> value) {
        if (StringUtils.startsWithIgnoreCase(value.getKey(), Constants.CompareExchange.RVN_ATOMIC_PREFIX)) {
            throw new IllegalStateException("'" + value.getKey() + "' is an atomic guard and you cannot load it via the session");
        }

        if (session.noTracking) {
            return new CompareExchangeSessionValue(value);
        }

        CompareExchangeSessionValue sessionValue = _state.get(value.getKey());

        if (sessionValue == null) {
            sessionValue = new CompareExchangeSessionValue(value);
            _state.put(value.getKey(), sessionValue);
            return sessionValue;
        }

        sessionValue.updateValue(value, session.getConventions().getEntityMapper());

        return sessionValue;
    }

    private boolean tryGetCompareExchangeValueFromSession(String key, Reference<CompareExchangeSessionValue> valueRef) {
        CompareExchangeSessionValue value = _state.get(key);
        valueRef.value = value;
        return value != null;
    }

    public void prepareCompareExchangeEntities(InMemoryDocumentSessionOperations.SaveChangesData result) {
        if (_state.isEmpty()) {
            return;
        }

        for (Map.Entry<String, CompareExchangeSessionValue> kvp : _state.entrySet()) {
            ICommandData command = kvp.getValue().getCommand(session.getConventions());
            if (command == null) {
                continue;
            }

            result.getSessionCommands().add(command);
        }
    }

    public void updateState(String key, long index) {
        Reference<CompareExchangeSessionValue> sessionValueReference = new Reference<>();
        if (!tryGetCompareExchangeValueFromSession(key, sessionValueReference)) {
            return;
        }

        sessionValueReference.value.updateState(index);
    }
}
