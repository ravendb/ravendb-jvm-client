package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;

import java.io.IOException;

public class CompareExchangeResult<T> {
    private T value;
    private long index;
    private boolean successful;

    public static <T> CompareExchangeResult<T> parseFromString(Class<T> clazz, String responseString, DocumentConventions conventions) throws IOException {
        JsonNode response = conventions.getEntityMapper().readTree(responseString);

        JsonNode indexJson = response.get("Index");
        if (indexJson == null || indexJson.isNull()) {
            throw new IllegalStateException("Response is invalid. Index is missing");
        }

        long index = indexJson.asLong();

        boolean successful = response.get("Successful").asBoolean();
        ObjectNode raw = (ObjectNode) response.get("Value");

        T result = CompareExchangeValueResultParser.deserializeObject(clazz, raw, conventions);

        CompareExchangeResult<T> exchangeResult = new CompareExchangeResult<>();
        exchangeResult.index = index;
        exchangeResult.value = result;
        exchangeResult.successful = successful;
        return exchangeResult;

    }

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
}
