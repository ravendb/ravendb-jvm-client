package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TimeSeriesConfiguration {

    public final static char TIME_SERIES_ROLLUP_SEPARATOR = '@';

    private Map<String, TimeSeriesCollectionConfiguration> collections = new HashMap<>();
    private Duration policyCheckFrequency;
    private Map<String, Map<String, String[]>> namedValues;

    @JsonCreator
    public TimeSeriesConfiguration(@JsonProperty("Collections") Map<String, TimeSeriesCollectionConfiguration> collections,
                                   @JsonProperty("PolicyCheckFrequency") Duration policyCheckFrequency,
                                   @JsonProperty("NamedValues") Map<String, Map<String, String[]>> namedValues) {
        this.collections = collections;
        this.policyCheckFrequency = policyCheckFrequency;
        this.namedValues = namedValues;

        internalPostJsonDeserialization();
    }

    public TimeSeriesConfiguration() {
    }

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

    public Map<String, Map<String, String[]>> getNamedValues() {
        return namedValues;
    }

    public void setNamedValues(Map<String, Map<String, String[]>> namedValues) {
        this.namedValues = namedValues;
    }

    public String[] getNames(String collection, String timeSeries) {
        if (namedValues == null) {
            return null;
        }

        Map<String, String[]> timeSeriesHolder = namedValues.get(collection);
        if (timeSeriesHolder == null) {
            return null;
        }

        String[] names = timeSeriesHolder.get(timeSeries);
        if (names == null) {
            return null;
        }

        return names;
    }

    private void internalPostJsonDeserialization() {
        populateNamedValues();
        populatePolicies();
    }

    private void populatePolicies() {
        if (collections == null) {
            return;
        }

        TreeMap<String, TimeSeriesCollectionConfiguration> dic = new TreeMap<>(String::compareToIgnoreCase);
        for (Map.Entry<String, TimeSeriesCollectionConfiguration> kvp : collections.entrySet()) {
            dic.put(kvp.getKey(), kvp.getValue());
        }

        collections = dic;
    }

    private void populateNamedValues() {
        if (namedValues == null) {
            return;
        }

        // ensure ignore case
        TreeMap<String, Map<String, String[]>> dic = new TreeMap<>(String::compareToIgnoreCase);

        for (Map.Entry<String, Map<String, String[]>> kvp : namedValues.entrySet()) {
            TreeMap<String, String[]> valueMap = new TreeMap<>(String::compareToIgnoreCase);
            valueMap.putAll(kvp.getValue());
            dic.put(kvp.getKey(), valueMap);
        }

        namedValues = dic;
    }
}