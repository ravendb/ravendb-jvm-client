package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.NetISO8601Utils;

import java.io.IOException;
import java.util.*;

/**
 * Represents a batch operation for time series data, including appends, increments, and deletions,
 * to be performed on a single document’s time series.
 */
public final class TimeSeriesOperation {

    private TreeSet<AppendOperation> _appends;
    private List<DeleteOperation> _deletes;
    private TreeSet<IncrementOperation> _increments;
    /**
     * The name of the time series on which the operations are performed.
     * This field is mandatory and must be set before executing the {@link TimeSeriesBatchOperation}
     * that contains this {@link TimeSeriesOperation}.
     *
     * <p>The {@link #name} field identifies the time series within the document to which the batch operations apply.
     * If this field is not set, an exception will be thrown when attempting to execute the batch operation.</p>
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TimeSeriesOperation() {
    }

    public TimeSeriesOperation(String name) {
        this.name = name;
    }

    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Name", name);
        generator.writeFieldName("Appends");
        if (_appends != null) {
            generator.writeStartArray();
            for (AppendOperation append : _appends) {
                append.serialize(generator, conventions);
            }
            generator.writeEndArray();
        } else {
            generator.writeNull();
        }
        generator.writeFieldName("Deletes");
        if (_deletes != null) {
            generator.writeStartArray();
            for (DeleteOperation delete : _deletes) {
                delete.serialize(generator, conventions);
            }
            generator.writeEndArray();
        } else {
            generator.writeNull();
        }

        generator.writeFieldName("Increments");
        if (_increments != null) {
            generator.writeStartArray();
            for (IncrementOperation increment : _increments) {
                increment.serialize(generator, conventions);
            }
            generator.writeEndArray();
        } else {
            generator.writeNull();
        }

        generator.writeEndObject();
    }

    /**
     * Adds an incremental operation to the batch for the specified time series.
     * This operation increments the values at the given timestamp, supporting concurrent updates in a distributed environment.
     *
     * @param incrementOperation The increment operation to add, specifying the timestamp and values to increment.
     *
     * <p>Incremental time series in RavenDB are designed to handle concurrent updates from multiple nodes.
     * Each node maintains its own local changes for the specified timestamp, which are aggregated to provide a unified view.</p>
     *
     * @throws IllegalStateException Thrown if the number of values in the new operation does not match the number in an existing operation for the same timestamp.
     */
    public void increment(IncrementOperation incrementOperation) {
        if (_increments == null) {
            _increments = new TreeSet<>(Comparator.comparing(x -> x.getTimestamp().getTime()));
        }
        boolean added = _increments.add(incrementOperation);
        if (!added) {
            // element with given timestamp already exists - remove and retry add operation
            _increments
                    .stream()
                    .filter(x -> x.getTimestamp().getTime() == incrementOperation.getTimestamp().getTime())
                    .findFirst()
                    .ifPresent(toDelete -> _appends.remove(toDelete));

            _increments.add(incrementOperation);
        }
    }

    /**
     * Adds an append operation to the batch.
     * This operation adds new data points to the time series at a specific timestamp.
     *
     * @param appendOperation The append operation to add.
     */
    public void append(AppendOperation appendOperation) {
        if (_appends == null) {
            _appends = new TreeSet<>(Comparator.comparing(x -> x.getTimestamp().getTime()));
        }
        boolean added = _appends.add(appendOperation);
        if (!added) {
            // element with given timestamp already exists - remove and retry add operation
            _appends
                    .stream()
                    .filter(x -> x.getTimestamp().getTime() == appendOperation.getTimestamp().getTime())
                    .findFirst()
                    .ifPresent(toDelete -> _appends.remove(toDelete));

            _appends.add(appendOperation);
        }
    }

    /**
     * Adds a delete operation to the batch.
     * This operation removes data points within a specific range from the time series.
     *
     * @param deleteOperation The delete operation to add.
     */
    public void delete(DeleteOperation deleteOperation) {
        if (_deletes == null) {
            _deletes = new ArrayList<>();
        }
        _deletes.add(deleteOperation);
    }

    /**
     * Represents an append operation in a time series, allowing new data points to be added at specific timestamps.
     */
    public static final class AppendOperation {
        /**
         * The timestamp of the data point to be appended.
         * This specifies when the data point occurred.
         */
        private Date timestamp;
        /**
         * The values associated with the data point.
         * These are the numeric measurements recorded at the specified {@link #timestamp}.
         */
        private double[] values;
        /**
         * An optional tag for the data point.
         * The tag can provide additional context or metadata for the appended data, such as the source or a descriptive label.
         */
        private String tag;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public double[] getValues() {
            return values;
        }

        public void setValues(double[] values) {
            this.values = values;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public AppendOperation() {
        }

        public AppendOperation(Date timestamp, double[] values) {
            this.timestamp = timestamp;
            this.values = values;
        }

        public AppendOperation(Date timestamp, double[] values, String tag) {
            this.timestamp = timestamp;
            this.values = values;
            this.tag = tag;
        }

        public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("Timestamp", NetISO8601Utils.format(timestamp, true));
            generator.writeFieldName("Values");
            generator.writeStartArray();

            for (double value : values) {
                generator.writeNumber(value);
            }

            generator.writeEndArray();
            if (tag != null) {
                generator.writeStringField("Tag", tag);
            }

            generator.writeEndObject();
        }
    }

    /**
     * Represents a delete operation in a time series, allowing data points within a specific range to be removed.
     */
    public static final class DeleteOperation {
        /**
         * The start of the range for the delete operation.
         * Data points from this timestamp (inclusive) will be considered for deletion.
         * If {@code null}, the range starts from the beginning of the time series.
         */
        private Date from;
        /**
         * The end of the range for the delete operation.
         * Data points up to this timestamp (inclusive) will be considered for deletion.
         * If {@code null}, the range extends to the end of the time series.
         */
        private Date to;

        public DeleteOperation() {
        }

        public DeleteOperation(Date from, Date to) {
            this.from = from;
            this.to = to;
        }

        public Date getFrom() {
            return from;
        }

        public void setFrom(Date from) {
            this.from = from;
        }

        public Date getTo() {
            return to;
        }

        public void setTo(Date to) {
            this.to = to;
        }

        public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
            generator.writeStartObject();

            generator.writeStringField("From", from != null ? NetISO8601Utils.format(from, true) : null);
            generator.writeStringField("To", to != null ? NetISO8601Utils.format(to, true) : null);
            generator.writeEndObject();
        }
    }

    /**
     * Represents an increment operation in a time series, allowing values at a specific timestamp to be incremented.
     */
    public static class IncrementOperation {
        /**
         * The timestamp of the data point to be incremented.
         * This specifies the exact point in time where the values should be adjusted.
         */
        private Date timestamp;
        /**
         * The values to increment at the specified {@link #timestamp}.
         * Each value corresponds to a numeric field in the time series, and the increment operation adds the specified amount to the current values.
         */
        private double[] values;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public double[] getValues() {
            return values;
        }

        public void setValues(double[] values) {
            this.values = values;
        }

        public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("Timestamp", timestamp != null ? NetISO8601Utils.format(timestamp, true) : null);

            generator.writeFieldName("Values");
            generator.writeStartArray();

            for (double value : values) {
                generator.writeNumber(value);
            }

            generator.writeEndArray();

            generator.writeEndObject();
        }
    }
}
