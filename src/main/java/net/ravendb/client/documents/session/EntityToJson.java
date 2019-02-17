package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;

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

        ObjectMapper mapper = _session.getConventions().getEntityMapper();

        ObjectNode jsonNode = mapper.valueToTree(entity);

        writeMetadata(mapper, jsonNode, documentInfo);

        Class<?> clazz = entity.getClass();
        tryRemoveIdentityProperty(jsonNode, clazz, _session.getConventions());

        return jsonNode;
    }

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions) {
        return convertEntityToJson(entity, conventions, null, true);
    }

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions,
                                                 DocumentInfo documentInfo, boolean removeIdentityProperty ) {
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
    public Object convertToEntity(Class entityType, String id, ObjectNode document) {
        try {
            if (ObjectNode.class.equals(entityType)) {
                return document;
            }

            Object defaultValue = InMemoryDocumentSessionOperations.getDefaultValue(entityType);
            Object entity = defaultValue;

            String documentType =_session.getConventions().getJavaClass(id, document);
            if (documentType != null) {
                Class type = Class.forName(documentType);
                if (entityType.isAssignableFrom(type)) {
                    entity = _session.getConventions().getEntityMapper().treeToValue(document, type);
                }
            }

            if (entity == defaultValue) {
                entity = _session.getConventions().getEntityMapper().treeToValue(document, entityType);
            }

            if (id != null) {
                _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);
            }

            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Could not convert document " + id + " to entity of type " + entityType.getName(), e);
        }
    }

    public void populateEntity(Object entity, String id, ObjectNode document) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        try {
            _session.getConventions().getEntityMapper().updateValue(entity, document);
            _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);
        } catch (IOException e) {
            throw new IllegalStateException("Could not populate entity");
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
}
