package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class CopyTimeSeriesCommandData implements ICommandData {

    private final String _id;
    private final String _name;
    private String _changeVector;
    private final String _destinationId;
    private final String _destinationName;

    @Override
    public CommandType getType() {
        return CommandType.TIME_SERIES_COPY;
    }

    public CopyTimeSeriesCommandData(String sourceDocumentId,
                                     String sourceName,
                                     String destinationDocumentId,
                                     String destinationName) {
        if (StringUtils.isBlank(sourceDocumentId)) {
            throw new IllegalArgumentException("SourceDocumentId cannot be null or whitespace");
        }
        if (StringUtils.isBlank(sourceName)) {
            throw new IllegalArgumentException("SourceName cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(destinationDocumentId)) {
            throw new IllegalArgumentException("DestinationDocumentId cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("DestinationName cannot be null or whitespace.");
        }

        _id = sourceDocumentId;
        _name = sourceName;
        _destinationId = destinationDocumentId;
        _destinationName = destinationName;
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getChangeVector() {
        return _changeVector;
    }

    public String getDestinationId() {
        return _destinationId;
    }

    public String getDestinationName() {
        return _destinationName;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", _id);
        generator.writeStringField("Name", _name);
        generator.writeStringField("DestinationId", _destinationId);
        generator.writeStringField("DestinationName", _destinationName);
        generator.writeStringField("Type", "TimeSeriesCopy");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        // empty
    }
}
