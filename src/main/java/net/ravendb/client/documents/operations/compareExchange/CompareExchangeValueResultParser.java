package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Defaults;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompareExchangeValueResultParser<T> {

    public static <T> Map<String, CompareExchangeValue<T>> getValues(Class<T> clazz, String response, DocumentConventions conventions) throws IOException {
        JsonNode jsonResponse = conventions.getEntityMapper().readTree(response);

        Map<String, CompareExchangeValue<T>> results = new HashMap<>();

        JsonNode items = jsonResponse.get("Results");
        if (items == null || items.isNull()) {
            throw new IllegalStateException("Response is invalid. Results is missing.");
        }

        for (JsonNode item : items) {
            if (item == null) {
                throw new IllegalStateException("Response is invalid. Item is null");
            }

            JsonNode key = item.get("Key");

            if (key == null || key.isNull()) {
                throw new IllegalStateException("Response is invalid. Key is missing.");
            }

            JsonNode index = item.get("Index");
            if (index == null || index.isNull()) {
                throw new IllegalStateException("Response is invalid. Index is missing");
            }

            JsonNode raw = item.get("Value");
            if (raw == null || raw.isNull()) {
                throw new IllegalStateException("Response is invalid. Value is missing.");
            }

            if (clazz.isPrimitive() || String.class.equals(clazz)) {
                // simple
                T value;
                JsonNode rawValue = raw.get("Object");
                value = conventions.getEntityMapper().convertValue(rawValue, clazz);
                CompareExchangeValue<T> cmpValue = new CompareExchangeValue<>(key.textValue(), index.asLong(), value);
                results.put(key.textValue(), cmpValue);
            } else {

                JsonNode object = raw.get("Object");
                if (object == null || object.isNull()) {
                    results.put(key.textValue(), new CompareExchangeValue<>(key.textValue(), index.asLong(), Defaults.defaultValue(clazz)));
                } else {
                    T converted = conventions.getEntityMapper().convertValue(object, clazz);
                    results.put(key.textValue(), new CompareExchangeValue<>(key.textValue(), index.asLong(), converted));
                }
            }
        }

        return results;
    }

    public static <T> CompareExchangeValue<T> getValue(Class<T> clazz, String response, DocumentConventions conventions) throws IOException {
        if (response == null) {
            return null;
        }

        Map<String, CompareExchangeValue<T>> values = getValues(clazz, response, conventions);
        if (values.isEmpty()) {
            return null;
        }
        return values.values().iterator().next();
    }
}
