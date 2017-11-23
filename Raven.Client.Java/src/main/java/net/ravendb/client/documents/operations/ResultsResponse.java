package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.indexes.PutIndexResult;

public class ResultsResponse<T> {

    private T[] results;

    public T[] getResults() {
        return results;
    }

    public void setResults(T[] results) {
        this.results = results;
    }

    public static class GetIndexNamesResponse extends ResultsResponse<String> {

    }

    public static class PutIndexesResponse extends ResultsResponse<PutIndexResult> {

    }

    public static class GetIndexesResponse extends ResultsResponse<IndexDefinition> {

    }

    public static class GetIndexStatisticsResponse extends ResultsResponse<IndexStats> {

    }
}
