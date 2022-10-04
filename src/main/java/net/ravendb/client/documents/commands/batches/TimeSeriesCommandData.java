package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesOperation;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;
import java.util.List;

public abstract class TimeSeriesCommandData implements ICommandData {

    private String id;
    private String name;
    private TimeSeriesOperation timeSeries;

    private Boolean fromEtl;

    protected TimeSeriesCommandData(String documentId, String name) {
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
    public abstract CommandType getType();

    public TimeSeriesOperation getTimeSeries() {
        return timeSeries;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeFieldName("TimeSeries");
        timeSeries.serialize(generator, conventions);
        generator.writeObjectField("Type", getType());
        if (fromEtl != null)
        {
            generator.writeBooleanField("FromEtl", fromEtl);
        }
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }

}
