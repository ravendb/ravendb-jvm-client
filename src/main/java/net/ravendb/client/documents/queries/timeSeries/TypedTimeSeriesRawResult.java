package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;

public class TypedTimeSeriesRawResult<TValues> extends TimeSeriesQueryResult {
    @JsonProperty("Results")
    private TypedTimeSeriesEntry<TValues>[] results;

    public TypedTimeSeriesEntry<TValues>[] getResults() {
        return results;
    }

    public void setResults(TypedTimeSeriesEntry<TValues>[] results) {
        this.results = results;
    }
}
