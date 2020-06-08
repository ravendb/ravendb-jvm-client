package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.json.MetadataAsDictionary;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class CompareExchangeValueResultParser<T> {


    /* TODO do we want that?
    public static object ConvertToBlittable(object value, DocumentConventions conventions, JsonOperationContext context)
+        {
+            return ConvertToBlittable(value, conventions, context, conventions.Serialization.CreateSerializer());
+        }
+
+        public static object ConvertToBlittable(object value, DocumentConventions conventions, JsonOperationContext context, IJsonSerializer jsonSerializer)
+        {
+            if (value == null)
+                return null;
+
+            if (value is ValueType ||
+                value is string ||
+                value is BlittableJsonReaderArray)
+                return value;
+
+            if (value is IEnumerable enumerable && !(enumerable is IDictionary))
+            {
+                return enumerable.Cast<object>()
+                    .Select(v => ConvertToBlittable(v, conventions, context, jsonSerializer));
+            }
+
+            return conventions.Serialization.DefaultConverter.ToBlittable(value, context);
+        }
     */

    public static <T> Map<String, CompareExchangeValue<T>> getValues(Class<T> clazz, String response,
                                                                     boolean materializeMetadata,
                                                                     DocumentConventions conventions) throws IOException {
        Map<String, CompareExchangeValue<T>> results = new TreeMap<>(String::compareToIgnoreCase);
        if (response == null) { // 404
            return results;
        }

        JsonNode jsonResponse = conventions.getEntityMapper().readTree(response);

        JsonNode items = jsonResponse.get("Results");
        if (items == null || items.isNull()) {
            throw new IllegalStateException("Response is invalid. Results is missing.");
        }

        for (JsonNode item : items) {
            if (item == null) {
                throw new IllegalStateException("Response is invalid. Item is null");
            }

            CompareExchangeValue<T> value = getSingleValue(clazz, (ObjectNode) item, materializeMetadata, conventions);
            results.put(value.getKey(), value);
        }

        return results;
    }

    public static <T> CompareExchangeValue<T> getValue(Class<T> clazz, String response,
                                                       boolean materializeMetadata,
                                                       DocumentConventions conventions) throws IOException {
        if (response == null) {
            return null;
        }

        Map<String, CompareExchangeValue<T>> values = getValues(clazz, response, materializeMetadata, conventions);
        if (values.isEmpty()) {
            return null;
        }
        return values.values().iterator().next();
    }

    public static <T> CompareExchangeValue<T> getSingleValue(Class<T> clazz, ObjectNode item, boolean materializeMetadata, DocumentConventions conventions) {
        if (item == null || item.isNull()) {
            return null;
        }

        JsonNode keyNode = item.get("Key");

        if (keyNode == null || keyNode.isNull()) {
            throw new IllegalStateException("Response is invalid. Key is missing.");
        }

        JsonNode indexNode = item.get("Index");
        if (indexNode == null || indexNode.isNull()) {
            throw new IllegalStateException("Response is invalid. Index is missing");
        }

        JsonNode rawJsonNode = item.get("Value");
        if (rawJsonNode == null) {
            throw new IllegalStateException("Response is invalid. Value is missing.");
        }

        ObjectNode raw = rawJsonNode.isObject() ? (ObjectNode) rawJsonNode : null;

        String key = keyNode.asText();
        long index = indexNode.asLong();

        if (raw == null) {
            return new CompareExchangeValue<>(key, index, null);
        }

        MetadataAsDictionary metadata = null;
        /* TODO
            if (raw.TryGet(Constants.Documents.Metadata.Key, out BlittableJsonReaderObject bjro) && bjro != null)
            {
                metadata = materializeMetadata == false
                    ? new MetadataAsDictionary(bjro)
                    : MetadataAsDictionary.MaterializeFromBlittable(bjro);
            }
         */


        if (clazz.isPrimitive() || String.class.equals(clazz)) {
            // simple
            T value = null;

            if (raw != null) {
                JsonNode rawValue = raw.get("Object");
                value = conventions.getEntityMapper().convertValue(rawValue, clazz);
            }

            return new CompareExchangeValue<>(key, index, value, metadata);
        } else if (ObjectNode.class.equals(clazz)) {
            if (raw == null || !raw.has(Constants.CompareExchange.OBJECT_FIELD_NAME)) {
                return new CompareExchangeValue<>(key, index, null, metadata);
            }

            Object rawValue = raw.get(Constants.CompareExchange.OBJECT_FIELD_NAME);
            if (rawValue == null) {
                return new CompareExchangeValue<>(key, index, null, metadata);
            } else if (rawValue instanceof ObjectNode) {
                return new CompareExchangeValue<>(key, index, (T) rawValue, metadata);
            } else {
                return new CompareExchangeValue<>(key, index, (T) raw, metadata);
            }
        } else {
            JsonNode object = raw.get(Constants.CompareExchange.OBJECT_FIELD_NAME);
            if (object == null || object.isNull()) {
                return new CompareExchangeValue<>(key, index, Defaults.defaultValue(clazz), metadata);
            } else {
                T converted = conventions.getEntityMapper().convertValue(object, clazz);
                return new CompareExchangeValue<>(key, index, converted, metadata);
            }
        }
    }
}
