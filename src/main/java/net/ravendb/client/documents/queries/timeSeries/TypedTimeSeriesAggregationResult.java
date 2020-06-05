package net.ravendb.client.documents.queries.timeSeries;

public class TypedTimeSeriesAggregationResult<T> extends TimeSeriesQueryResult {
    private TypedTimeSeriesRangeAggregation<T>[] results;

    public TypedTimeSeriesRangeAggregation<T>[] getResults() {
        return results;
    }

    public void setResults(TypedTimeSeriesRangeAggregation<T>[] results) {
        this.results = results;
    }
}
