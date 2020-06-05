package net.ravendb.client.documents.operations.timeSeries;

public class ConfigureRawTimeSeriesPolicyOperation extends ConfigureTimeSeriesPolicyOperation {
    public ConfigureRawTimeSeriesPolicyOperation(String collection, RawTimeSeriesPolicy config) {
        super(collection, config);
    }
}
