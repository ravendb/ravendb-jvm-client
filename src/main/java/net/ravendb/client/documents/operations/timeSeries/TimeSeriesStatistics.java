package net.ravendb.client.documents.operations.timeSeries;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesStatistics {

    private String documentId;
    private List<TimeSeriesItemDetail> timeSeries;

    public TimeSeriesStatistics() {
        timeSeries = new ArrayList<>();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<TimeSeriesItemDetail> getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(List<TimeSeriesItemDetail> timeSeries) {
        this.timeSeries = timeSeries;
    }

}
