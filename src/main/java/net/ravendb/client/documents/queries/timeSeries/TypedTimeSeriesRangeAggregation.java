package net.ravendb.client.documents.queries.timeSeries;

import java.util.Date;

public class TypedTimeSeriesRangeAggregation<T> {

    private T max;

    private T min;

    private T last;

    private T first;

    private T average;

    private T sum;

    private T count;

    private Date to;

    private Date from;

    public T getCount() {
        return count;
    }

    public void setCount(T count) {
        this.count = count;
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public T getLast() {
        return last;
    }

    public void setLast(T last) {
        this.last = last;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public T getAverage() {
        return average;
    }

    public void setAverage(T average) {
        this.average = average;
    }

    public T getSum() {
        return sum;
    }

    public void setSum(T sum) {
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
}
