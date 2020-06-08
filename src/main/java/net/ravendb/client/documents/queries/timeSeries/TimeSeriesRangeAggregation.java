package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;

import java.util.Arrays;
import java.util.Date;

public class TimeSeriesRangeAggregation {
    @JsonProperty("Count")
    private long[] count;

    @JsonProperty("Max")
    private double[] max;

    @JsonProperty("Min")
    private double[] min;

    @JsonProperty("Last")
    private double[] last;

    @JsonProperty("First")
    private double[] first;

    @JsonProperty("Average")
    private double[] average;

    @JsonProperty("Sum")
    private double[] sum;

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

    public double[] getMax() {
        return max;
    }

    public void setMax(double[] max) {
        this.max = max;
    }

    public double[] getMin() {
        return min;
    }

    public void setMin(double[] min) {
        this.min = min;
    }

    public double[] getLast() {
        return last;
    }

    public void setLast(double[] last) {
        this.last = last;
    }

    public double[] getFirst() {
        return first;
    }

    public void setFirst(double[] first) {
        this.first = first;
    }

    public double[] getAverage() {
        return average;
    }

    public void setAverage(double[] average) {
        this.average = average;
    }

    public double[] getSum() {
        return sum;
    }

    public void setSum(double[] sum) {
        this.sum = sum;
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

    public <T> TypedTimeSeriesRangeAggregation<T> asTypedEntry(Class<T> clazz) {
        TypedTimeSeriesRangeAggregation<T> typedEntry = new TypedTimeSeriesRangeAggregation<>();

        //TODO: what about sum?

        typedEntry.setFrom(from);
        typedEntry.setTo(to);
        typedEntry.setMin(min != null ? TimeSeriesValuesHelper.setFields(clazz, min, true) : null);
        typedEntry.setMax(max != null ? TimeSeriesValuesHelper.setFields(clazz, max, true) : null);
        typedEntry.setFirst(first != null ? TimeSeriesValuesHelper.setFields(clazz, first, true) : null);
        typedEntry.setLast(last != null ? TimeSeriesValuesHelper.setFields(clazz, last, true) : null);
        double[] counts = Arrays.stream(count).asDoubleStream().toArray();
        typedEntry.setCount(count != null ? TimeSeriesValuesHelper.setFields(clazz, counts, true) : null);
        typedEntry.setAverage(average != null ? TimeSeriesValuesHelper.setFields(clazz, average, true) : null);

        return typedEntry;
    }
}
