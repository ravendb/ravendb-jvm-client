package net.ravendb.client.documents.operations.timeSeries;

public abstract class AbstractTimeSeriesRange {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
