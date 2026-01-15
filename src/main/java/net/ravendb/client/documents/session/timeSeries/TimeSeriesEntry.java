package net.ravendb.client.documents.session.timeSeries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import net.ravendb.client.documents.operations.timeSeries.GetTimeSeriesOperation;
import java.util.Date;
import java.util.Map;

/**
 * Represents an individual entry in a time series, storing a timestamp, associated values, and metadata.
 *
 * <p>{@link TimeSeriesEntry} is used as a base type for time series data projections
 * in operations such as {@link GetTimeSeriesOperation}.
 * It contains a timestamp and associated values for a single entry within a time series.</p>
 */
public class TimeSeriesEntry {
    /**
     * Gets or sets the timestamp of the time series entry.
     * This represents the point in time at which the values were recorded
     */
    @JsonProperty("Timestamp")
    private Date timestamp;
    /**
     * Gets or sets the tag for the time series entry.
     * This optional metadata provides additional context about the entry, such as the source or a descriptive label.
     */
    @JsonProperty("Tag")
    private String tag;
    /**
     * Gets or sets the values associated with the time series entry.
     * These values represent the numeric data points recorded at the specified <{@link Date}.
     */
    @JsonProperty("Values")
    private double[] values;
    /**
     * Gets or sets a value indicating whether the entry is part of a rollup time series.
     * A rollup aggregates data over a specific time range, as opposed to individual data points.
     */
    @JsonProperty("IsRollup")
    private boolean rollup;
    /**
     * Gets or sets a dictionary of node-specific values for the time series entry.
     * The key represents the node identifier, and the value is an array of numeric data points associated with that node.
     */
    private Map<String, Double[]> nodeValues;

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

    public Map<String, Double[]> getNodeValues() {
        return nodeValues;
    }

    public void setNodeValues(Map<String, Double[]> nodeValues) {
        this.nodeValues = nodeValues;
    }
    /**
     * Gets the value of the time series entry if it contains a single value.
     *
     * @throws IllegalStateException Thrown if the entry contains more than one value.
     */
    @JsonIgnore
    public double getValue() {
        if (values.length == 1) {
            return values[0];
        }

        throw new IllegalStateException("Entry has more than one value.");
    }
    /**
     * Sets the value of the time series entry if it contains a single value.
     *
     * @throws IllegalStateException Thrown if the entry contains more than one value.
     */

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

    public String toString() {
        return "[" + timestamp + "] " + StringUtils.join(values, ',') + " " + tag;
    }
}
