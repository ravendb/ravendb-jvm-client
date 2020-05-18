package net.ravendb.client.documents.operations.timeSeries;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesCollectionConfiguration {

    private boolean disabled;

    private List<TimeSeriesPolicy> policies = new ArrayList<>();
    private RawTimeSeriesPolicy rawPolicy = RawTimeSeriesPolicy.DEFAULT_POLICY;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Specify roll up and retention policy.
     * Each policy will create a new time-series aggregated from the previous one
     * @return roll up policies
     */
    public List<TimeSeriesPolicy> getPolicies() {
        return policies;
    }

    /**
     * Specify roll up and retention policy.
     * Each policy will create a new time-series aggregated from the previous one
     * @param policies roll up policies to use
     */
    public void setPolicies(List<TimeSeriesPolicy> policies) {
        this.policies = policies;
    }

    /**
     * Specify a policy for the original time-series
     * @return raw time series policy
     */
    public RawTimeSeriesPolicy getRawPolicy() {
        return rawPolicy;
    }

    /**
     * Specify a policy for the original time-series
     * @param rawPolicy raw time series policy to use
     */
    public void setRawPolicy(RawTimeSeriesPolicy rawPolicy) {
        this.rawPolicy = rawPolicy;
    }
}
