package net.ravendb.client.documents.operations.timeSeries;

import java.util.Date;

/**
 * Represents a range of time series data based on specific start and end times.
 */
public class TimeSeriesRange extends AbstractTimeSeriesRange {
    /**
     * The start time of the range.
     * Data points from this timestamp (inclusive) will be included in the range.
     * If {@code null}, the range starts from the beginning of the time series.
     */
    private Date from;
    /**
     * The end time of the range.
     * Data points up to this timestamp (inclusive) will be included in the range.
     * If {@code null}, the range extends to the end of the time series.
     */
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
