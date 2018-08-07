package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetDocumentsResult {

    private ObjectNode includes;
    private ArrayNode results;
    private ObjectNode counterIncludes;
    private int nextPageStart;

    public ObjectNode getIncludes() {
        return includes;
    }

    public void setIncludes(ObjectNode includes) {
        this.includes = includes;
    }

    public ArrayNode getResults() {
        return results;
    }

    public void setResults(ArrayNode results) {
        this.results = results;
    }

    public int getNextPageStart() {
        return nextPageStart;
    }

    public void setNextPageStart(int nextPageStart) {
        this.nextPageStart = nextPageStart;
    }

    public ObjectNode getCounterIncludes() {
        return counterIncludes;
    }

    public void setCounterIncludes(ObjectNode counterIncludes) {
        this.counterIncludes = counterIncludes;
    }
}
