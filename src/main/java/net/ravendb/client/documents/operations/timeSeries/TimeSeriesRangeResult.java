package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.Date;

public class TimeSeriesRangeResult {
    private Date from;
    private Date to;
    private TimeSeriesEntry[] entries;
    private Long totalResults;

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
}
