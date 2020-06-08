package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.Date;
import java.util.List;

public class TimeSeriesRangeResult {
    private Date from; //TODO: make nullable on server side - Karmel says we don't have min/max here - we trim to stored values!
    private Date to; // TODO make nullable on server side - Karmel says we don't have min/max here - we trim to stored values!
    private List<TimeSeriesEntry> entries;
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

    public List<TimeSeriesEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<TimeSeriesEntry> entries) {
        this.entries = entries;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
    }
}
