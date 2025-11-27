package net.ravendb.client.documents.operations.timeSeries;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of executing a {@link GetMultipleTimeSeriesOperation},
 * providing details of time series data associated with a document in RavenDB.
 */
public class TimeSeriesDetails {
    /**
     * The ID of the document to which the time series data belongs.
     *
     * <p>This property identifies the document that holds the time series data retrieved by the operation.</p>
     */
    private String _id;
    /**
     * A dictionary containing the time series data.
     *
     * <p>The dictionary maps the name of each time series to a list of {@link TimeSeriesRangeResult} objects,
     * where each range result represents a segment of the time series data.
     * The data is retrieved as part of the {@link GetMultipleTimeSeriesOperation}.</p>
     */
    private Map<String, List<TimeSeriesRangeResult>> _values;

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public Map<String, List<TimeSeriesRangeResult>> getValues() {
        return _values;
    }

    public void setValues(Map<String, List<TimeSeriesRangeResult>> values) {
        _values = values;
    }
}
