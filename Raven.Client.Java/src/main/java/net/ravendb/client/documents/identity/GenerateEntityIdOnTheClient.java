package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.function.Function;

public class GenerateEntityIdOnTheClient {

    private final DocumentConventions _conventions;
    private final Function<Object, String> _generateId;

    public GenerateEntityIdOnTheClient(DocumentConventions conventions, Function<Object, String> generateId) {
        this._conventions = conventions;
        this._generateId = generateId;
    }

    private Field getIdentityProperty(Class<?> entityType) {
        return _conventions.getIdentityProperty(entityType);
    }

    /**
     * Attempts to get the document key from an instance
     * @param entity Entity to get id from
     * @param idHolder output parameter which holds document id
     * @return true if id was read from entity
     */
    public boolean tryGetIdFromInstance(Object entity, Reference<String> idHolder) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        try {
            Field identityProperty = getIdentityProperty(entity.getClass());
            if (identityProperty != null) {
                Object value = FieldUtils.readField(identityProperty, entity, true);
                if (value instanceof String) {
                    idHolder.value = (String)value;
                    return true;
                }
            }
            idHolder.value = null;
            return false;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Tries to get the identity.
     * @param entity Entity
     * @return Document id
     */
    public String getOrGenerateDocumentId(Object entity) {
        Reference<String> idHolder = new Reference<>();
        tryGetIdFromInstance(entity, idHolder);
        String id = idHolder.value;
        if (id == null) {
            // Generate the key up front
            id = _generateId.apply(entity);
        }

        if (id != null && id.startsWith("/")) {
            throw new IllegalStateException("Cannot use value '" + id + "' as a document id because it begins with a '/'");
        }
        return id;
    }

    public String generateDocumentKeyForStorage(Object entity) {
        String id = getOrGenerateDocumentId(entity);
        trySetIdentity(entity, id);
        return id;
    }

    /**
     * Tries to set the identity property
     * @param entity Entity
     * @param id Id to set
     */
    public void trySetIdentity(Object entity, String id) {
        Class<?> entityType = entity.getClass();
        Field identityProperty = _conventions.getIdentityProperty(entityType);

        if (identityProperty == null) {
            return;
        }

        setPropertyOrField(identityProperty.getType(), entity, identityProperty, id);
    }

    private void setPropertyOrField(Class<?> propertyOrFieldType, Object entity, Field field, String id) {
        try {
            if (String.class.equals(propertyOrFieldType)) {
                FieldUtils.writeField(field, entity, id, true);
            } else {
                throw new IllegalArgumentException("Cannot set identity value '" + id + "' on field " + propertyOrFieldType +
                        " because field type is not string.");
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
