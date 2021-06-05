package net.ravendb.client.documents.operations.timeSeries;

public class TimeSeriesCountRange extends AbstractTimeSeriesRange {
    private int count;
    private TimeSeriesRangeType type;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public TimeSeriesRangeType getType() {
        return type;
    }

    public void setType(TimeSeriesRangeType type) {
        this.type = type;
    }
}
