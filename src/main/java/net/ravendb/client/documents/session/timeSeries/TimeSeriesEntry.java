package net.ravendb.client.documents.session.timeSeries;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class TimeSeriesEntry {
    @JsonProperty("Timestamp")
    private Date timestamp;

    @JsonProperty("Tag")
    private String tag;

    @JsonProperty("Values")
    private double[] values;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    public double getValue() {
        return values[0];
    }
}
