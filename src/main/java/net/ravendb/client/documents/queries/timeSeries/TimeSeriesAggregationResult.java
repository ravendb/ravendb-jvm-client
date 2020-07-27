package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class TimeSeriesAggregationResult extends TimeSeriesQueryResult {
    @JsonProperty("Results")
    private TimeSeriesRangeAggregation[] results;

    public TimeSeriesRangeAggregation[] getResults() {
        return results;
    }

    public void setResults(TimeSeriesRangeAggregation[] results) {
        this.results = results;
    }

    @SuppressWarnings("unchecked")
    public <T> TypedTimeSeriesAggregationResult<T> asTypedResult(Class<T> clazz) {
        TypedTimeSeriesAggregationResult<T> result = new TypedTimeSeriesAggregationResult<>();
        result.setCount(getCount());

        result.setResults(Arrays.stream(results)
                .map(x -> x.asTypedEntry(clazz))
                .toArray(TypedTimeSeriesRangeAggregation[]::new));

        return result;
    }
}
