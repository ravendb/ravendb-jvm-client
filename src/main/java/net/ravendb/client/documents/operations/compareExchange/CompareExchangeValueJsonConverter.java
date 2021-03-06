package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.EntityToJson;
import org.apache.commons.lang3.ClassUtils;

public class CompareExchangeValueJsonConverter {
    public static Object convertToJson(Object value, DocumentConventions conventions) {
        if (value == null) {
            return null;
        }

        if (ClassUtils.isPrimitiveOrWrapper(value.getClass()) || value instanceof String || value.getClass().isArray()) {
            return value;
        }

        return EntityToJson.convertEntityToJson(value, conventions, null);
    }
}
