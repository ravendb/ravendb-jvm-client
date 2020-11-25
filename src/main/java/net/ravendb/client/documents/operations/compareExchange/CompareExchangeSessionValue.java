package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.DeleteCompareExchangeCommandData;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.PutCompareExchangeCommandData;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.exceptions.RavenException;
import org.apache.commons.lang3.ClassUtils;

import java.util.Date;

public class CompareExchangeSessionValue {

    private final String _key;
    private long _index;
    private CompareExchangeValue<ObjectNode> _originalValue;

    private ICompareExchangeValue _value;
    private CompareExchangeValueState _state;

    public CompareExchangeSessionValue(String key, long index, CompareExchangeValueState state) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        _key = key;
        _index = index;
        _state = state;
    }

    public CompareExchangeSessionValue(CompareExchangeValue<ObjectNode> value) {
        this(value.getKey(), value.getIndex(), value.getIndex() >= 0 ? CompareExchangeValueState.NONE : CompareExchangeValueState.MISSING);

        if (value.getIndex() > 0) {
            _originalValue = value;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> CompareExchangeValue<T> getValue(Class<T> clazz, DocumentConventions conventions) {
        switch (_state) {
            case NONE:
            case CREATED:
                if (_value instanceof CompareExchangeValue) {
                    return (CompareExchangeValue<T>) _value;
                }

                if (_value != null) {
                    throw new IllegalStateException("Value cannot be null");
                }

                T entity = null;
                if (_originalValue != null && _originalValue.getValue() != null) {
                    if (ClassUtils.isPrimitiveOrWrapper(clazz) || String.class.equals(clazz)) {
                        try {
                            JsonNode entityJsonValue = _originalValue.getValue().get(Constants.CompareExchange.OBJECT_FIELD_NAME);
                            entity = conventions.getEntityMapper().treeToValue(entityJsonValue, clazz);
                        } catch (JsonProcessingException ex) {
                            throw new RavenException("Unable to read compare exchange value: " + _originalValue.getValue(), ex);
                        }
                    } else {
                        entity = (T) EntityToJson.convertToEntity(clazz, _key, _originalValue.getValue(), conventions);
                    }
                }

                CompareExchangeValue<T> value = new CompareExchangeValue<>(_key, _index, entity);
                _value = value;

                return value;
            case MISSING:
            case DELETED:
                return null;
            default:
                throw new UnsupportedOperationException("Not supported state: " + _state);
        }
    }

    public <T> CompareExchangeValue<T> create(T item) {
        assertState();

        if (_value != null) {
            throw new IllegalStateException("The compare exchange value with key '" + _key + "' is already tracked.");
        }

        _index = 0;
        CompareExchangeValue<T> value = new CompareExchangeValue<>(_key, _index, item);
        _value = value;
        _state = CompareExchangeValueState.CREATED;
        return value;
    }

    public void delete(long index) {
        assertState();

        _index = index;
        _state = CompareExchangeValueState.DELETED;
    }

    private void assertState() {
        switch (_state) {
            case NONE:
            case MISSING:
                return;
            case CREATED:
                throw new IllegalStateException("The compare exchange value with key '" + _key + "' was already stored.");
            case DELETED:
                throw new IllegalStateException("The compare exchange value with key '" + _key + "' was already deleted.");
        }
    }

    public ICommandData getCommand(DocumentConventions conventions) {
        switch (_state) {
            case NONE:
            case CREATED:
                if (_value == null) {
                    return null;
                }

                Object entity = CompareExchangeValueJsonConverter.convertToJson(_value.getValue(), conventions);

                ObjectNode entityJson = entity instanceof ObjectNode ? (ObjectNode) entity : null;
                ObjectNode metadata = null;
                if (_value.hasMetadata() && _value.getMetadata().size() != 0) {
                    metadata = prepareMetadataForPut(_key, _value.getMetadata(), conventions);
                }
                ObjectNode entityToInsert = null;
                if (entityJson == null) {
                    entityJson = entityToInsert = convertEntity(_key, entity, conventions.getEntityMapper(), metadata);
                }

                CompareExchangeValue<ObjectNode> newValue = new CompareExchangeValue<>(_key, _index, entityJson);
                boolean hasChanged = _originalValue == null || hasChanged(_originalValue, newValue);
                _originalValue = newValue;

                if (!hasChanged) {
                    return null;
                }

                if (entityToInsert == null) {
                    entityToInsert = convertEntity(_key, entity, conventions.getEntityMapper(), metadata);
                }

                return new PutCompareExchangeCommandData(newValue.getKey(), entityToInsert, newValue.getIndex());
            case DELETED:
                return new DeleteCompareExchangeCommandData(_key, _index);
            case MISSING:
                return null;
            default:
                throw new IllegalStateException("Not supported state: " + _state);
        }
    }


    private ObjectNode convertEntity(String key, Object entity, ObjectMapper objectMapper, ObjectNode metadata) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.set(Constants.CompareExchange.OBJECT_FIELD_NAME, objectMapper.valueToTree(entity));
        if (metadata != null) {
            objectNode.set(Constants.Documents.Metadata.KEY, metadata);
        }
        return objectNode;
    }

    public boolean hasChanged(CompareExchangeValue<ObjectNode> originalValue, CompareExchangeValue<ObjectNode> newValue) {
        if (originalValue == newValue) {
            return false;
        }

        if (!originalValue.getKey().equalsIgnoreCase(newValue.getKey())) {
            throw new IllegalStateException("Keys do not match. Expected '" + originalValue.getKey() + " but was: " + newValue.getKey());
        }

        if (originalValue.getIndex() != newValue.getIndex()) {
            return true;
        }

        return !originalValue.getValue().equals(newValue.getValue());
    }

    public void updateState(long index) {
        _index = index;
        _state = CompareExchangeValueState.NONE;

        if (_originalValue != null) {
            _originalValue.setIndex(index);
        }

        if (_value != null) {
            _value.setIndex(index);
        }
    }

    public void updateValue(CompareExchangeValue<ObjectNode> value, ObjectMapper mapper) {
        _index = value.getIndex();
        _state = value.getIndex() >= 0 ? CompareExchangeValueState.NONE : CompareExchangeValueState.MISSING;

        _originalValue = value;

        if (_value != null) {
            _value.setIndex(_index);

            if (_value.getValue() != null) {
                EntityToJson.populateEntity(_value.getValue(), value.getValue(), mapper);
            }
        }
    }

    public static ObjectNode prepareMetadataForPut(String key, IMetadataDictionary metadataDictionary, DocumentConventions conventions) {
        if (metadataDictionary.containsKey(Constants.Documents.Metadata.EXPIRES)) {
            Object obj = metadataDictionary.get(Constants.Documents.Metadata.EXPIRES);
            if (obj == null) {
                throwInvalidExpiresMetadata("The values of " + Constants.Documents.Metadata.EXPIRES + " metadata for compare exchange '" + key + " is null.");
            }
            if (!(obj instanceof Date) && !(obj instanceof String)) {
                throwInvalidExpiresMetadata("The class of " + Constants.Documents.Metadata.EXPIRES + " metadata for compare exchange '" + key + " is not valid. Use the following type: Date or string.");
            }
        }

        return conventions.getEntityMapper().convertValue(metadataDictionary, ObjectNode.class);
    }

    private static void throwInvalidExpiresMetadata(String message) {
        throw new IllegalArgumentException(message);
    }
}
