package net.ravendb.client.json;

import com.fasterxml.jackson.databind.node.*;
import net.ravendb.client.documents.session.IMetadataDictionary;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

public class MetadataAsDictionary implements IMetadataDictionary {

    private Map<String, Object> _metadata;
    private final ObjectNode _source;

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

    private void init() {
        _metadata = new HashMap<>();
        Iterator<String> fields = _source.fieldNames();
        while (fields.hasNext()) {
            String fieldName = fields.next();
            _metadata.put(fieldName, convertValue(_source.get(fieldName)));
        }
    }

    private static Object convertValue(Object value) {
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
            return new MetadataAsDictionary((ObjectNode)value);
        }

        if (value instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) value;
            Object[] result = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = convertValue(array.get(i));
            }
            return result;
        }

        throw new NotImplementedException("Implement support for numbers and more");
    }

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
            init();
        }

        return _metadata.put(key, value);
    }

    @Override
    public Object get(Object key) {
        if (_metadata != null) {
            return _metadata.get(key);
        }

        return convertValue(_source.get((String) key));
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (_metadata == null) {
            init();
        }

        _metadata.putAll(m);
    }

    @Override
    public void clear() {
        if (_metadata == null) {
            init();
        }

        _metadata.clear();
    }

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
            init();
        }

        return _metadata.entrySet();
    }

    @Override
    public Object remove(Object key) {
        if (_metadata == null) {
            init();
        }

        return _metadata.remove(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (_metadata == null) {
            init();
        }

        return _metadata.containsValue(value);
    }

    @Override
    public Collection<Object> values() {
        if (_metadata == null) {
            init();
        }

        return _metadata.values();
    }

    @Override
    public Set<String> keySet() {
        if (_metadata == null) {
            init();
        }

        return _metadata.keySet();
    }
}
