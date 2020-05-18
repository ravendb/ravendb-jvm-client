package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.primitives.TimeValue;
import org.apache.commons.lang3.StringUtils;

public class TimeSeriesPolicy {

    protected String _name;
    protected TimeValue _retentionTime;
    protected TimeValue _aggregationTime;

    protected TimeSeriesPolicy() {
    }

    /**
     * @return  Name of the time series policy, must be unique.
     */
    public String getName() {
        return _name;
    }

    /**
     * @param name  Name of the time series policy, must be unique.
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * @return How long the data of this policy will be retained
     */
    public TimeValue getRetentionTime() {
        return _retentionTime;
    }

    /**
     * @param retentionTime How long the data of this policy will be retained
     */
    public void setRetentionTime(TimeValue retentionTime) {
        _retentionTime = retentionTime;
    }

    /**
     * @return Define the aggregation of this policy
     */
    public TimeValue getAggregationTime() {
        return _aggregationTime;
    }

    /**
     * @param aggregationTime Define the aggregation of this policy
     */
    public void setAggregationTime(TimeValue aggregationTime) {
        _aggregationTime = aggregationTime;
    }

    public String getTimeSeriesName(String rawName) {
        return rawName + TimeSeriesConfiguration.TIME_SERIES_ROLLUP_SEPARATOR + _name;
    }

    public TimeSeriesPolicy(String name, TimeValue aggregationTime) {
        this(name, aggregationTime, TimeValue.MAX_VALUE);
    }

    public TimeSeriesPolicy(String name, TimeValue aggregationTime, TimeValue retentionTime) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        if (aggregationTime.compareTo(TimeValue.ZERO) <= 0) {
            throw new IllegalArgumentException("Aggregation time must be greater than zero");
        }

        if (retentionTime.compareTo(TimeValue.ZERO) <= 0) {
            throw new IllegalArgumentException("Retention time must be greater than zero");
        }

        _retentionTime = retentionTime;
        _aggregationTime = aggregationTime;

        _name = name;
    }
}
