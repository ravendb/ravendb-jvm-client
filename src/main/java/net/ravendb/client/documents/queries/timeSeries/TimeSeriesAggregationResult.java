package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeSeriesAggregationResult extends TimeSeriesQueryResult {
    @JsonProperty("Results")
    private TimeSeriesRangeAggregation[] results;

    public TimeSeriesRangeAggregation[] getResults() {
        return results;
    }

    public void setResults(TimeSeriesRangeAggregation[] results) {
        this.results = results;
    }
}
