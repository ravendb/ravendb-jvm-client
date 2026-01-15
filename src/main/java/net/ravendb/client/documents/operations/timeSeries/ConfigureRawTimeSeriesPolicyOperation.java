package net.ravendb.client.documents.operations.timeSeries;

/**
 * Inherits documentation from {@link ConfigureTimeSeriesPolicyOperation}.
 */
public class ConfigureRawTimeSeriesPolicyOperation extends ConfigureTimeSeriesPolicyOperation {
    /**
     * Inherits documentation from {@link ConfigureTimeSeriesPolicyOperation#ConfigureTimeSeriesPolicyOperation(String, TimeSeriesPolicy)}.
     */
    public ConfigureRawTimeSeriesPolicyOperation(String collection, RawTimeSeriesPolicy config) {
        super(collection, config);
    }
}
