package net.ravendb.client.documents.operations;


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

}
