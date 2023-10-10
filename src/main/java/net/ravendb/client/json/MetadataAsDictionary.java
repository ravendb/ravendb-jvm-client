package net.ravendb.client.json;

import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.Sets;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.IMetadataDictionary;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class MetadataAsDictionary implements IMetadataDictionary {

    private static final Set<String> RESERVED_METADATA_PROPERTIES = Sets.newHashSet(
            Constants.Documents.Metadata.COLLECTION,
            Constants.Documents.Metadata.ID,
            Constants.Documents.Metadata.CHANGE_VECTOR,
            Constants.Documents.Metadata.LAST_MODIFIED,
            Constants.Documents.Metadata.RAVEN_JAVA_TYPE
    );

    private MetadataAsDictionary _parent;
    private String _parentKey;

    private Map<String, Object> _metadata;
    private final ObjectNode _source;

    private boolean _hasChanges;

    public MetadataAsDictionary(ObjectNode metadata) {
        this._source = metadata;
    }

    public MetadataAsDictionary() {
        this(new HashMap<>());
    }

    public MetadataAsDictionary(Map<String, Object> metadata) {
        _metadata = metadata;
        _source = null;
    }

    public MetadataAsDictionary(ObjectNode metadata, MetadataAsDictionary parent, String parentKey) {
        this(metadata);

        if (parent == null) {
            throw new IllegalArgumentException("Parent cannot be null");
        }

        if (parentKey == null) {
            throw new IllegalArgumentException("ParentKey cannot be null");
        }

        _parent = parent;
        _parentKey = parentKey;
    }

    public boolean isDirty() {
        return _metadata != null && _hasChanges;
    }

    @SuppressWarnings("ConstantConditions")
    private void initialize(ObjectNode metadata) {
        _metadata = new HashMap<>();
        Iterator<String> fields = metadata.fieldNames();
        while (fields.hasNext()) {
            String fieldName = fields.next();
            _metadata.put(fieldName, convertValue(fieldName, metadata.get(fieldName)));
        }
    }

    private Object convertValue(String key, Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return value;
        }

        if (value instanceof Boolean) {
            return value;
        }

        if (value instanceof TextNode) {
            return ((TextNode) value).asText();
        }

        if (value instanceof NumericNode) {
            return ((NumericNode) value).numberValue();
        }

        if (value instanceof Double) {
            return value;
        }

        if (value instanceof Float) {
            return value;
        }

        if (value instanceof ObjectNode) {
            MetadataAsDictionary dictionary = new MetadataAsDictionary((ObjectNode) value, this, key);
            dictionary.initialize((ObjectNode) value);
            return dictionary;
        }

        if (value instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) value;
            Object[] result = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = convertValue(key, array.get(i));
            }
            return result;
        }

        throw new NotImplementedException("Implement support for numbers and more");
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int size() {
        if (_metadata != null) {
            return _metadata.size();
        }

        return _source.size();
    }

    @Override
    public Object put(String key, Object value) {
        if (_metadata == null) {
            initialize(_source);
        }

        Object currentValue = _metadata.get(key);
        if (currentValue == null || !currentValue.equals(value)) {
            _metadata.put(key, value);
            markChanged();
        }

        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Object get(Object key) {
        if (_metadata != null) {
            return _metadata.get(key);
        }

        return convertValue((String) key, _source.get((String) key));
    }

    public static MetadataAsDictionary materializeFromJson(ObjectNode metadata) {
        MetadataAsDictionary result = new MetadataAsDictionary((Map<String, Object>) null);
        result.initialize(metadata);

        return result;
    }

    @Override
    public IMetadataDictionary[] getObjects(String key) {
        Object[] obj = (Object[]) get(key);
        if (obj == null) {
            return null;
        }
        IMetadataDictionary[] list = new IMetadataDictionary[obj.length];
        for (int i = 0; i < obj.length; i++) {
            list[i] = (IMetadataDictionary) obj[i];
        }

        return list;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (_metadata == null) {
            initialize(_source);
        }

        markChanged();

        _metadata.putAll(m);
    }

    @Override
    public void clear() {
        if (_metadata == null) {
            initialize(_source);
        }

        Set<String> keysToRemove = new HashSet<>();

        for (Entry<String, Object> item : _metadata.entrySet()) {
            if (RESERVED_METADATA_PROPERTIES.contains(item.getKey())) {
                continue;
            }
            keysToRemove.add(item.getKey());
        }

        if (!keysToRemove.isEmpty()) {
            for (String s : keysToRemove) {
                _metadata.remove(s);
            }

            markChanged();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean containsKey(Object key) {
        if (_metadata != null) {
            return _metadata.containsKey(key);
        }

        return _source.has((String)key);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        if (_metadata == null) {
            initialize(_source);
        }

        return _metadata.entrySet();
    }

    @Override
    public Object remove(Object key) {
        if (_metadata == null) {
            initialize(_source);
        }

        Object oldValue = _metadata.remove(key);

        if (oldValue != null) {
            markChanged();
        }

        return oldValue;
    }

    @Override
    public boolean containsValue(Object value) {
        if (_metadata == null) {
            initialize(_source);
        }

        return _metadata.containsValue(value);
    }

    @Override
    public Collection<Object> values() {
        if (_metadata == null) {
            initialize(_source);
        }

        return _metadata.values();
    }

    @Override
    public Set<String> keySet() {
        if (_metadata == null) {
            initialize(_source);
        }

        return _metadata.keySet();
    }

    @Override
    public String getString(String key) {
        Object obj = get(key);
        return obj != null ? obj.toString() : null;
    }

    @Override
    public long getLong(String key) {
        Object obj = get(key);
        if (obj == null) {
            return 0L;
        }

        if (obj instanceof Long) {
            return (Long) obj;
        }

        return Long.parseLong(obj.toString());
    }

    @Override
    public boolean getBoolean(String key) {
        Object obj = get(key);
        if (obj == null) {
            return false;
        }

        if (obj instanceof Boolean) {
            return (boolean) obj;
        }

        return Boolean.parseBoolean(obj.toString());
    }

    @Override
    public double getDouble(String key) {
        Object obj = get(key);
        if (obj == null) {
            return 0;
        }

        if (obj instanceof Double) {
            return (Double) obj;
        }

        return Double.parseDouble(obj.toString());
    }

    @Override
    public IMetadataDictionary getObject(String key) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }

        return (IMetadataDictionary) obj;
    }

    protected void markChanged() {
        _hasChanges = true;

        if (_parent == null) {
            return;
        }

        _parent.put(_parentKey, this);
        _parent.markChanged();
    }
}
