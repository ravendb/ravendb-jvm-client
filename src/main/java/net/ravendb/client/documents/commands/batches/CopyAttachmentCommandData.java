package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class CopyAttachmentCommandData implements ICommandData {

    private String id;
    private String name;
    private String destinationId;
    private String destinationName;
    private String changeVector;


    public CopyAttachmentCommandData(String sourceDocumentId, String sourceName, String destinationDocumentId, String destinationName, String changeVector) {
        if (StringUtils.isBlank(sourceDocumentId)) {
            throw new IllegalArgumentException("SourceDocumentId is required");
        }

        if (StringUtils.isBlank(sourceName)) {
            throw new IllegalArgumentException("SourceName is required");
        }

        if (StringUtils.isBlank(destinationDocumentId)) {
            throw new IllegalArgumentException("DestinationDocumentId is required");
        }

        if (StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("DestinationName is required");
        }

        id = sourceDocumentId;
        name = sourceName;
        destinationId = destinationDocumentId;
        this.destinationName = destinationName;
        this.changeVector = changeVector;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    @Override
    public String getChangeVector() {
        return changeVector;
    }

    @Override
    public CommandType getType() {
        return CommandType.ATTACHMENT_COPY;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("Name", name);
        generator.writeStringField("DestinationId", destinationId);
        generator.writeStringField("DestinationName", destinationName);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeStringField("Type", "AttachmentCOPY");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
