package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;

public class CmpXchgResult<T> {
    private T value;
    private long index;
    private boolean successful;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public static <T> CmpXchgResult<T> parseFromString(Class<T> clazz, String stringResponse) throws IOException {
        JsonNode response = JsonExtensions.getDefaultEntityMapper().readTree(stringResponse);
        if (!response.has("Index")) {
            throw new IllegalStateException("Response is invalid");
        }

        int index = response.get("Index").asInt();
        boolean successful = response.get("Successful").asBoolean();
        ObjectNode raw = (ObjectNode) response.get("Value");

        T result;
        Object val = null;
        if (raw != null) {
            val = raw.get("Object");
        }

        if (val == null) {
            CmpXchgResult<T> xchgResult = new CmpXchgResult<>();
            xchgResult.setIndex(index);
            xchgResult.setValue(Defaults.defaultValue(clazz));
            xchgResult.setSuccessful(successful);
            return xchgResult;
        }

        try {
            if (val instanceof ObjectNode) {
                result = JsonExtensions.getDefaultEntityMapper().treeToValue((ObjectNode) val, clazz);
            } else {
                result = JsonExtensions.getDefaultEntityMapper().treeToValue(raw.get("Object"), clazz);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse json: " + e.getMessage(), e);
        }

        CmpXchgResult<T> xchgResult = new CmpXchgResult<>();
        xchgResult.setIndex(index);
        xchgResult.setValue(result);
        xchgResult.setSuccessful(successful);
        return xchgResult;
    }
}
