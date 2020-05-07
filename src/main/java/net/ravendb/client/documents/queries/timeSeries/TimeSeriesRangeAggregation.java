package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class TimeSeriesRangeAggregation {
    @JsonProperty("Count")
    private long[] count;

    @JsonProperty("Max")
    private Double[] max;

    @JsonProperty("Min")
    private Double[] min;

    @JsonProperty("Last")
    private Double[] last;

    @JsonProperty("First")
    private Double[] first;

    @JsonProperty("Average")
    private Double[] average;

    @JsonProperty("To")
    private Date to;

    @JsonProperty("From")
    private Date from;

    public long[] getCount() {
        return count;
    }

    public void setCount(long[] count) {
        this.count = count;
    }

    public Double[] getMax() {
        return max;
    }

    public void setMax(Double[] max) {
        this.max = max;
    }

    public Double[] getMin() {
        return min;
    }

    public void setMin(Double[] min) {
        this.min = min;
    }

    public Double[] getLast() {
        return last;
    }

    public void setLast(Double[] last) {
        this.last = last;
    }

    public Double[] getFirst() {
        return first;
    }

    public void setFirst(Double[] first) {
        this.first = first;
    }

    public Double[] getAverage() {
        return average;
    }

    public void setAverage(Double[] average) {
        this.average = average;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }
}
