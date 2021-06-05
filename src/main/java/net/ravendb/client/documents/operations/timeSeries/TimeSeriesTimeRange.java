package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.primitives.TimeValue;

public class TimeSeriesTimeRange extends AbstractTimeSeriesRange {
    private TimeValue time;
    private TimeSeriesRangeType type;

    public TimeSeriesRangeType getType() {
        return type;
    }

    public void setType(TimeSeriesRangeType type) {
        this.type = type;
    }

    public TimeValue getTime() {
        return time;
    }

    public void setTime(TimeValue time) {
        this.time = time;
    }
}
