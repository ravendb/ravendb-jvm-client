package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesOperation;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;
import java.util.List;

public class TimeSeriesBatchCommandData implements ICommandData {

    private String id;
    private String name;
    private TimeSeriesOperation timeSeries;

    public TimeSeriesBatchCommandData(String documentId, String name, List<TimeSeriesOperation.AppendOperation> appends,
                                      List<TimeSeriesOperation.RemoveOperation> removals) {
        if (documentId == null) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        this.id = documentId;
        this.name = name;

        this.timeSeries = new TimeSeriesOperation();
        this.timeSeries.setName(name);

        if (appends != null) {
            for (TimeSeriesOperation.AppendOperation appendOperation : appends) {
                timeSeries.append(appendOperation);
            }
        }

        if (removals != null) {
            for (TimeSeriesOperation.RemoveOperation removeOperation : removals) {
                timeSeries.remove(removeOperation);
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getChangeVector() {
        return null;
    }

    @Override
    public CommandType getType() {
        return CommandType.TIME_SERIES;
    }

    public TimeSeriesOperation getTimeSeries() {
        return timeSeries;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeFieldName("TimeSeries");
        timeSeries.serialize(generator, conventions);
        generator.writeObjectField("Type", "TimeSeries");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }

}
