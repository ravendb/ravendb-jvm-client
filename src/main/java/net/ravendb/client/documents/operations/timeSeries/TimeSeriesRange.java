package net.ravendb.client.documents.operations.timeSeries;

import java.util.Date;

public class TimeSeriesRange {
    private String name;
    private Date from;
    private Date to;

    public TimeSeriesRange() {
    }

    public TimeSeriesRange(String name, Date from, Date to) {
        this.name = name;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
