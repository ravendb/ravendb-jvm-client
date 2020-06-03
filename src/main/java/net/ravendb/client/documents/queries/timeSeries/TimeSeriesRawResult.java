package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;

import java.util.Arrays;

public class TimeSeriesRawResult extends TimeSeriesQueryResult {
    @JsonProperty("Results")
    private TimeSeriesEntry[] results;

    public TimeSeriesEntry[] getResults() {
        return results;
    }

    public void setResults(TimeSeriesEntry[] results) {
        this.results = results;
    }

    public <T> TypedTimeSeriesRawResult<T> asTypedResult(Class<T> clazz) {
        TypedTimeSeriesRawResult<T> result = new TypedTimeSeriesRawResult<>();
        result.setCount(getCount());
        result.setResults(Arrays
                .stream(results)
                .map(x -> x.asTypedEntry(clazz))
                .toArray(TypedTimeSeriesEntry[]::new));
        return result;
    }
}
