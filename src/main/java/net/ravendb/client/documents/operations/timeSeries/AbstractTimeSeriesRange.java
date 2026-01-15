package net.ravendb.client.documents.operations.timeSeries;

/**
 * Represents a base class for defining time series ranges.
 */
public abstract class AbstractTimeSeriesRange {
    /**
     * The name of the time series.
     * <p>
     * This field identifies the time series on which the range operations will be performed.
     */
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
