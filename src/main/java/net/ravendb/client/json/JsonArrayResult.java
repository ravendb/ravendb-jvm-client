package net.ravendb.client.json;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonArrayResult {
    private ArrayNode results;

    public ArrayNode getResults() {
        return results;
    }

    public void setResults(ArrayNode results) {
        this.results = results;
    }
}
