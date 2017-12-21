package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

public class EntityToJson {

    private final InMemoryDocumentSessionOperations _session;

    /**
     * All the listeners for this session
     */
    public EntityToJson(InMemoryDocumentSessionOperations _session) {
        this._session = _session;
    }

    private final Map<Object, Map<String, Object>> missingDictionary = new TreeMap<>((o1, o2) -> o1 == o2 ? 0 : 1);

    public Map<Object, Map<String, Object>> getMissingDictionary() {
        return missingDictionary;
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
        //TBD: TrySimplifyJson(reader);

        return jsonNode;
    }

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions) {
        return convertEntityToJson(entity, conventions, null);
    }

    public static ObjectNode convertEntityToJson(Object entity, DocumentConventions conventions, DocumentInfo documentInfo) {
        // maybe we don't need to do anything?
        if (entity instanceof ObjectNode) {
            return (ObjectNode) entity;
        }

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        ObjectNode jsonNode = mapper.valueToTree(entity);

        writeMetadata(mapper, jsonNode, documentInfo);

        Class<?> clazz = entity.getClass();
        tryRemoveIdentityProperty(jsonNode, clazz, conventions);
        //TBD: TrySimplifyJson(reader);

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
            documentInfo.getMetadata().fieldNames().forEachRemaining(property -> {
                metadataNode.set(property, documentInfo.getMetadata().get(property).deepCopy());
            });
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

    //TBD public static object ConvertToEntity(Type entityType, string id, BlittableJsonReaderObject document, DocumentConventions conventions)

    private static boolean tryRemoveIdentityProperty(ObjectNode document, Class entityType, DocumentConventions conventions) {
        Field identityProperty = conventions.getIdentityProperty(entityType);

        if (identityProperty == null) {
            return false;
        }

        document.remove(identityProperty.getName());

        return true;
    }
}
