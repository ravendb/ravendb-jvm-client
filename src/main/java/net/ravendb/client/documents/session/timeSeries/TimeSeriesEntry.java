package net.ravendb.client.documents.session.timeSeries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class TimeSeriesEntry {
    @JsonProperty("Timestamp")
    private Date timestamp;

    @JsonProperty("Tag")
    private String tag;

    @JsonProperty("Values")
    private double[] values;

    @JsonProperty("IsRollup")
    private boolean rollup;

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

    public boolean isRollup() {
        return rollup;
    }

    public void setRollup(boolean rollup) {
        this.rollup = rollup;
    }

    @JsonIgnore
    public double getValue() {
        if (values.length == 1) {
            return values[0];
        }

        throw new IllegalStateException("Entry has more than one value.");
    }

    @JsonIgnore
    public void setValue(double value) {
        if (values.length == 1) {
            values[0] = value;
            return;
        }

        throw new IllegalStateException("Entry has more than one value.");
    }

    public <T> TypedTimeSeriesEntry<T> asTypedEntry(Class<T> clazz) {
        TypedTimeSeriesEntry<T> entry = new TypedTimeSeriesEntry<>();
        entry.setRollup(rollup);
        entry.setTag(tag);
        entry.setTimestamp(timestamp);
        entry.setValues(values);
        entry.setValue(TimeSeriesValuesHelper.setFields(clazz, values, rollup));
        return entry;
    }

}
