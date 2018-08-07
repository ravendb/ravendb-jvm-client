package net.ravendb.client.json;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class BatchCommandResult {
    private ArrayNode results;
    private Long transactionIndex;

    public ArrayNode getResults() {
        return results;
    }

    public void setResults(ArrayNode results) {
        this.results = results;
    }

    public Long getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(Long transactionIndex) {
        this.transactionIndex = transactionIndex;
    }
}
