package net.ravendb.client.documents.operations.timeSeries;

import java.util.List;
import java.util.Map;

public class TimeSeriesDetails {
    private String _id;
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
