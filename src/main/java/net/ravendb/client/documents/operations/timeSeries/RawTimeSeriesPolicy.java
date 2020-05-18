package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.primitives.TimeValue;

public class RawTimeSeriesPolicy extends TimeSeriesPolicy {

    private final static String POLICY_STRING = "rawpolicy"; // must be lower case

    public static final RawTimeSeriesPolicy DEFAULT_POLICY = new RawTimeSeriesPolicy();

    public RawTimeSeriesPolicy() {
        _name = POLICY_STRING;
        _retentionTime = TimeValue.MAX_VALUE;
    }

    public RawTimeSeriesPolicy(TimeValue retentionTime) {
        if (retentionTime.compareTo(TimeValue.ZERO) <= 0) {
            throw new IllegalArgumentException("Must be greater than zero");
        }

        _name = POLICY_STRING;
        _retentionTime = retentionTime;
    }

}
