package net.ravendb.client.documents.session.timeSeries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class TypedTimeSeriesEntry<T> {
    @JsonProperty("Timestamp")
    private Date timestamp;

    @JsonProperty("Tag")
    private String tag;

    @JsonProperty("Values")
    private double[] values;

    @JsonProperty("IsRollup")
    private boolean rollup;

    @JsonIgnore
    private T value;

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

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isRollup() {
        return rollup;
    }

    public void setRollup(boolean rollup) {
        this.rollup = rollup;
    }
}
