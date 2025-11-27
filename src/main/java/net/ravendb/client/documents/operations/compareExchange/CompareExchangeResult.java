package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;

import java.io.IOException;

/**
 * Represents the result of a delete/put compare-exchange operation,
 * containing the value, index, and success status.
 *
 * @param <T> The type of the value stored in the compare-exchange result.
 */
public class CompareExchangeResult<T> {
    /**
     * The value associated with the compare-exchange operation.
     */
    private T value;
    /**
     * The index of the compare-exchange.
     */
    private long index;
    /**
     * Indicates whether the compare-exchange operation was successful.
     */
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
