package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.indexes.PutIndexResult;

public class PutIndexesResponse {
    private PutIndexResult[] results;

    public PutIndexResult[] getResults() {
        return results;
    }

    public void setResults(PutIndexResult[] results) {
        this.results = results;
    }
}
