package net.ravendb.client.documents.operations.timeSeries;

import java.time.Duration;
import java.util.Map;

public class TimeSeriesConfiguration {

    public final static char TIME_SERIES_ROLLUP_SEPARATOR = '@';

    private Map<String, TimeSeriesCollectionConfiguration> collections;
    private Duration policyCheckFrequency = Duration.ofMinutes(10);

    public Map<String, TimeSeriesCollectionConfiguration> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, TimeSeriesCollectionConfiguration> collections) {
        this.collections = collections;
    }

    public Duration getPolicyCheckFrequency() {
        return policyCheckFrequency;
    }

    public void setPolicyCheckFrequency(Duration policyCheckFrequency) {
        this.policyCheckFrequency = policyCheckFrequency;
    }

}