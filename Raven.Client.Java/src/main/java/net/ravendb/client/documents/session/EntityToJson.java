package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.stream.StreamSupport;

public class EntityToJson {

    private final InMemoryDocumentSessionOperations _session;

    /**
     * All the listeners for this session
     */
    public EntityToJson(InMemoryDocumentSessionOperations _session) {
        this._session = _session;
    }

    /* TODO
        public readonly Dictionary<object, Dictionary<string, object>> MissingDictionary = new Dictionary<object, Dictionary<string, object>>(ObjectReferenceEqualityComparer<object>.Default);
*/

    public ObjectNode convertEntityToJson(Object entity, DocumentInfo documentInfo) {
        // maybe we don't need to do anything?
        if (entity instanceof ObjectNode) {
            return (ObjectNode) entity;
        }

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        ObjectNode jsonNode = mapper.valueToTree(entity);

        writeMetadata(mapper, jsonNode, documentInfo);

        Class<?> clazz = entity.getClass();
        tryRemoveIdentityProperty(jsonNode, clazz, _session.getConventions());
        //TODO: TrySimplifyJson(reader);

        return jsonNode;
    }

    private void writeMetadata(ObjectMapper mapper, ObjectNode jsonNode, DocumentInfo documentInfo) {
        if (documentInfo == null) {
            return;
        }
        boolean setMetadata = false;
        ObjectNode metadataNode = mapper.createObjectNode();

        if (documentInfo.getMetadata() != null && documentInfo.getMetadata().size() > 0) {
            setMetadata = true;
            documentInfo.getMetadata().fieldNames().forEachRemaining(property -> {
                if (property.length() > 0 && property.startsWith("@")) {
                    if (!property.equals(Constants.Documents.Metadata.COLLECTION) && !Constants.Documents.Metadata.EXPIRES.equals(property)) {
                        return;
                    }
                }

                metadataNode.set(property, documentInfo.getMetadata().get(property));
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
     * Converts a BlittableJsonReaderObject to an entity.
     */
    public Object convertToEntity(Class entityType, String id, ObjectNode document) {
        try {
            if (ObjectNode.class.equals(entityType)) {
                return document;
            }

            Object defaultValue = InMemoryDocumentSessionOperations.getDefaultValue(entityType);
            Object entity = defaultValue;

            Class documentType =_session.getConventions().getJavaClass(id, document);
            if (documentType != null) {
                if (entityType.isAssignableFrom(documentType)) {
                    entity = _session.getConventions().deserializeEntityFromJson(documentType, document);
                }
            }

            /* TODO:
               if (Equals(entity, defaultValue))
                {
                    entity = _session.Conventions.DeserializeEntityFromBlittable(entityType, document);
                }

                if (id != null)
                    _session.GenerateEntityIdOnTheClient.TrySetIdentity(entity, id);

             */
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Could not convert document " + id + " to entity of type " + entityType.getName(), e);
        }
    }

    /* TODO


        /// <summary>
        /// Converts a BlittableJsonReaderObject to an entity without a session.
        /// </summary>
        /// <param name="entityType"></param>
        /// <param name="id">The id.</param>
        /// <param name="document">The document found.</param>
        /// <param name="conventions">The conventions.</param>
        /// <returns>The converted entity</returns>
        public static object ConvertToEntity(Type entityType, string id, BlittableJsonReaderObject document, DocumentConventions conventions)
        {
            try
            {
                var defaultValue = InMemoryDocumentSessionOperations.GetDefaultValue(entityType);
                var entity = defaultValue;

                var documentType = conventions.GetClrType(id, document);
                if (documentType != null)
                {
                    var type = Type.GetType(documentType);
                    if (type != null && entityType.IsAssignableFrom(type))
                    {
                        entity = conventions.DeserializeEntityFromBlittable(type, document);
                    }
                }

                if (Equals(entity, defaultValue))
                {
                    entity = conventions.DeserializeEntityFromBlittable(entityType, document);
                }

                return entity;
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException($"Could not convert document {id} to entity of type {entityType}",
                    ex);
            }
        }*/

    private static boolean tryRemoveIdentityProperty(ObjectNode document, Class entityType, DocumentConventions conventions) {
        Field identityProperty = conventions.getIdentityProperty(entityType);

        if (identityProperty == null) {
            return false;
        }

        document.remove(StringUtils.capitalize(identityProperty.getName()));

        return true;
    }


    /* TODO

        private static bool TrySimplifyJson(BlittableJsonReaderObject document)
        {
            var simplified = false;
            foreach (var propertyName in document.GetPropertyNames())
            {
                var propertyValue = document[propertyName];

                var propertyArray = propertyValue as BlittableJsonReaderArray;
                if (propertyArray != null)
                {
                    simplified |= TrySimplifyJson(propertyArray);
                    continue;
                }

                var propertyObject = propertyValue as BlittableJsonReaderObject;
                if (propertyObject == null)
                    continue;

                string type;
                if (propertyObject.TryGet(Constants.Json.Fields.Type, out type) == false)
                {
                    simplified |= TrySimplifyJson(propertyObject);
                    continue;
                }

                if (ShouldSimplifyJsonBasedOnType(type) == false)
                    continue;

                simplified = true;

                if (document.Modifications == null)
                    document.Modifications = new DynamicJsonValue(document);

                BlittableJsonReaderArray values;
                if (propertyObject.TryGet(Constants.Json.Fields.Values, out values) == false)
                {
                    if (propertyObject.Modifications == null)
                        propertyObject.Modifications = new DynamicJsonValue(propertyObject);

                    propertyObject.Modifications.Remove(Constants.Json.Fields.Type);
                    continue;
                }

                document.Modifications[propertyName] = values;

                simplified |= TrySimplifyJson(values);
            }

            return simplified;
        }

        private static bool TrySimplifyJson(BlittableJsonReaderArray array)
        {
            var simplified = false;
            foreach (var item in array)
            {
                var itemObject = item as BlittableJsonReaderObject;
                if (itemObject == null)
                    continue;

                simplified |= TrySimplifyJson(itemObject);
            }

            return simplified;
        }

        private static readonly Regex ArrayEndRegex = new Regex(@"\[\], [\w\.-]+$", RegexOptions.Compiled);

        private static bool ShouldSimplifyJsonBasedOnType(string typeValue)
        {
            if (typeValue == null)
                return false;
            if (typeValue.StartsWith("System.Collections.Generic.List`1[["))
                return true;
            if (typeValue.StartsWith("System.Collections.Generic.Dictionary`2[["))
                return true;
            if (ArrayEndRegex.IsMatch(typeValue)) // array
                return true;
            return false;
        }
     */
}
