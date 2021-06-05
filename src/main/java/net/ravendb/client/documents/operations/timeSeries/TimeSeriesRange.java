package net.ravendb.client.documents.operations.timeSeries;

import java.util.Date;

public class TimeSeriesRange extends AbstractTimeSeriesRange {
    private Date from;
    private Date to;

    public TimeSeriesRange() {
    }

    public TimeSeriesRange(String name, Date from, Date to) {
        setName(name);
        this.from = from;
        this.to = to;
    }

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
}
