package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.Reference;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class EntityToJson {

    private final InMemoryDocumentSessionOperations _session;

    /**
     * All the listeners for this session
     * @param _session Session to use
     */
    public EntityToJson(InMemoryDocumentSessionOperations _session) {
        this._session = _session;
    }

    private final Map<Object, Map<String, Object>> _missingDictionary = new HashMap<>();

    public Map<Object, Map<String, Object>> getMissingDictionary() {
        return _missingDictionary;
    }

    public ObjectNode convertEntityToJson(Object entity, DocumentInfo documentInfo) {
        // maybe we don't need to do anything?
        if (entity instanceof ObjectNode) {
            return (ObjectNode) entity;
        }

        if (documentInfo != null) {
            _session.onBeforeConversionToDocumentInvoke(documentInfo.getId(), entity);
        }

        ObjectNode document = convertEntityToJsonInternal(entity, _session.getConventions(), documentInfo);

        if (documentInfo != null) {
            Reference<ObjectNode> documentReference = new Reference<>(document);
            _session.onAfterConversionToDocumentInvoke(documentInfo.getId(), entity, documentReference);
            document = documentReference.value;
        }

        return document;
    }

    //TODO: fill missing properties?

    //TODO: internal static object ConvertToBlittableForCompareExchangeIfNeeded(

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions, DocumentInfo documentInfo) {
        return convertEntityToJsonInternal(entity, conventions, documentInfo);
    }

    private static ObjectNode convertEntityToJsonInternal(Object entity, DocumentConventions conventions, DocumentInfo documentInfo) {
        return convertEntityToJsonInternal(entity, conventions, documentInfo, true);
    }

    private static ObjectNode convertEntityToJsonInternal(Object entity, DocumentConventions conventions, DocumentInfo documentInfo, boolean removeIdentityProperty) {
        ObjectMapper mapper = conventions.getEntityMapper();

        ObjectNode jsonNode = mapper.valueToTree(entity);

        writeMetadata(mapper, jsonNode, documentInfo);

        Class<?> clazz = entity.getClass();
        if (removeIdentityProperty) {
            tryRemoveIdentityProperty(jsonNode, clazz, conventions);
        }

        return jsonNode;
    }

    //TODO: private void RegisterMissingProperties(object o, string id, object value)

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions,
                                                 DocumentInfo documentInfo, boolean removeIdentityProperty) {
        // maybe we don't need to do anything?
        if (entity instanceof ObjectNode) {
            return (ObjectNode) entity;
        }

        ObjectMapper mapper = conventions.getEntityMapper();

        ObjectNode jsonNode = mapper.valueToTree(entity);

        writeMetadata(mapper, jsonNode, documentInfo);

        Class<?> clazz = entity.getClass();

        if (removeIdentityProperty) {
            tryRemoveIdentityProperty(jsonNode, clazz, conventions);
        }

        return jsonNode;
    }

    private static void writeMetadata(ObjectMapper mapper, ObjectNode jsonNode, DocumentInfo documentInfo) {
        if (documentInfo == null) {
            return;
        }
        boolean setMetadata = false;
        ObjectNode metadataNode = mapper.createObjectNode();

        if (documentInfo.getMetadata() != null && documentInfo.getMetadata().size() > 0) {
            setMetadata = true;
            documentInfo.getMetadata().fieldNames().forEachRemaining(property -> metadataNode.set(property, documentInfo.getMetadata().get(property).deepCopy()));
        } else if (documentInfo.getMetadataInstance() != null) {
            setMetadata = true;
            for (Map.Entry<String, Object> entry : documentInfo.getMetadataInstance().entrySet()) {
                metadataNode.set(entry.getKey(), mapper.valueToTree(entry.getValue()));
            }
        }

        if (documentInfo.getCollection() != null) {
            setMetadata = true;

            metadataNode.set(Constants.Documents.Metadata.COLLECTION, mapper.valueToTree(documentInfo.getCollection()));
        }

        if (setMetadata) {
            jsonNode.set(Constants.Documents.Metadata.KEY, metadataNode);
        }
    }

    /**
     * Converts a json object to an entity.
     * @param entityType Class of entity
     * @param id Id of entity
     * @param document Raw entity
     * @return Entity instance
     */
    @SuppressWarnings("unchecked")
    public Object convertToEntity(Class entityType, String id, ObjectNode document, boolean trackEntity) {
        try {
            if (ObjectNode.class.equals(entityType)) {
                return document;
            }

            Reference<ObjectNode> documentRef = new Reference<>(document);
            _session.onBeforeConversionToEntityInvoke(id, entityType, documentRef);
            document = documentRef.value;

            Object defaultValue = InMemoryDocumentSessionOperations.getDefaultValue(entityType);
            Object entity = defaultValue;

            //TODO: if track! -> RegisterMissingProperties

            String documentType =_session.getConventions().getJavaClass(id, document);
            if (documentType != null) {
                Class type = _session.getConventions().getJavaClassByName(documentType);
                if (entityType.isAssignableFrom(type)) {
                    entity = _session.getConventions().getEntityMapper().treeToValue(document, type);
                }
            }

            if (entity == defaultValue) {
                entity = _session.getConventions().getEntityMapper().treeToValue(document, entityType);
            }

            JsonNode projectionNode = document.get(Constants.Documents.Metadata.PROJECTION);
            boolean isProjection = projectionNode != null && projectionNode.isBoolean() && projectionNode.asBoolean();

            if (id != null) {
                _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id, isProjection);
            }

            _session.onAfterConversionToEntityInvoke(id, document, entity);

            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Could not convert document " + id + " to entity of type " + entityType.getName(), e);
        }
    }

    public void populateEntity(Object entity, String id, ObjectNode document) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        populateEntity(entity, document, _session.getConventions().getEntityMapper());

        _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);
    }

    public static void populateEntity(Object entity, ObjectNode document, ObjectMapper objectMapper) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper cannot be null");
        }

        try {
            objectMapper.updateValue(entity, document);
        } catch (IOException e) {
            throw new IllegalStateException("Could not populate entity", e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean tryRemoveIdentityProperty(ObjectNode document, Class entityType, DocumentConventions conventions) {
        Field identityProperty = conventions.getIdentityProperty(entityType);

        if (identityProperty == null) {
            return false;
        }

        document.remove(identityProperty.getName());

        return true;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static Object convertToEntity(Class<?> entityClass, String id, ObjectNode document, DocumentConventions conventions) {
        try {

            Object defaultValue = InMemoryDocumentSessionOperations.getDefaultValue(entityClass);

            Object entity = defaultValue;

            String documentType = conventions.getJavaClass(id, document);
            if (documentType != null) {
                Class<?> clazz = Class.forName(documentType);
                if (clazz != null && entityClass.isAssignableFrom(clazz)) {
                    entity = conventions.getEntityMapper().treeToValue(document, clazz);
                }
            }

            if (entity == null) {
                entity = conventions.getEntityMapper().treeToValue(document, entityClass);
            }

            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Could not convert document " + id + " to entity of type " + entityClass, e);
        }
    }

    public void removeFromMissing(Object entity) {
        _missingDictionary.remove(entity);
    }

    public void clear() {
        _missingDictionary.clear();
    }

}
