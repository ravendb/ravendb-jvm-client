package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.primitives.NetISO8601Utils;

import java.io.IOException;
import java.util.*;

public class TimeSeriesOperation {

    private TreeSet<AppendOperation> _appends;
    private List<RemoveOperation> _removals;
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
        generator.writeFieldName("Removals");
        if (_removals != null) {
            generator.writeStartArray();
            for (RemoveOperation removal : _removals) {
                removal.serialize(generator, conventions);
            }
            generator.writeEndArray();
        } else {
            generator.writeNull();
        }
        generator.writeEndObject();
    }

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

    public void remove(RemoveOperation removeOperation) {
        if (_removals == null) {
            _removals = new ArrayList<>();
        }
        _removals.add(removeOperation);
    }

    public static class AppendOperation {
        private Date timestamp;
        private double[] values;
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

    public static class RemoveOperation {
        private Date from;
        private Date to;

        public RemoveOperation() {
        }

        public RemoveOperation(Date from, Date to) {
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

}
