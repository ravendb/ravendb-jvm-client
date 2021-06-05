package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.Date;

public class TimeSeriesRangeResult {
    private Date from;
    private Date to;
    private TimeSeriesEntry[] entries;
    private Long totalResults;
    private ObjectNode includes;

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public TimeSeriesEntry[] getEntries() {
        return entries;
    }

    public void setEntries(TimeSeriesEntry[] entries) {
        this.entries = entries;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
    }

    public ObjectNode getIncludes() {
        return includes;
    }

    public void setIncludes(ObjectNode includes) {
        this.includes = includes;
    }
}
