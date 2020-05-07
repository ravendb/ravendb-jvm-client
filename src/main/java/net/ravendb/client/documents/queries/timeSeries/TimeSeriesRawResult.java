package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

public class TimeSeriesRawResult extends TimeSeriesQueryResult {
    @JsonProperty("Results")
    private TimeSeriesEntry[] results;

    public TimeSeriesEntry[] getResults() {
        return results;
    }

    public void setResults(TimeSeriesEntry[] results) {
        this.results = results;
    }
}
