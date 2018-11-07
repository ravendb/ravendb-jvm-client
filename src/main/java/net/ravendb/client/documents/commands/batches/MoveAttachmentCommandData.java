package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class MoveAttachmentCommandData implements ICommandData {

    private String id;
    private String name;
    private String destinationId;
    private String destinationName;
    private String changeVector;

    public MoveAttachmentCommandData(String documentId, String name, String destinationDocumentId, String destinationName, String changeVector) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId is required");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        if (StringUtils.isBlank(destinationDocumentId)) {
            throw new IllegalArgumentException("DestinationDocumentId is required");
        }

        if (StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("DestinationName is required");
        }

        this.id = documentId;
        this.name = name;
        this.destinationId = destinationDocumentId;
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
        return CommandType.ATTACHMENT_MOVE;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("Name", name);
        generator.writeStringField("DestinationId", destinationId);
        generator.writeStringField("DestinationName", destinationName);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeStringField("Type", "AttachmentMOVE");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
