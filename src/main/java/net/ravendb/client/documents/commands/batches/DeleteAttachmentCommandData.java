package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class DeleteAttachmentCommandData implements ICommandData {

    private String id;
    private String name;
    private String changeVector;
    private CommandType type = CommandType.ATTACHMENT_DELETE;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getChangeVector() {
        return changeVector;
    }

    @Override
    public CommandType getType() {
        return type;
    }

    public DeleteAttachmentCommandData(String documentId, String name, String changeVector) {
        if (StringUtils.isWhitespace(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (StringUtils.isWhitespace(name)) {
            throw new IllegalArgumentException("Namem cannot be null");
        }

        this.id = documentId;
        this.name = name;
        this.changeVector = changeVector;
    }

    @Override
    public void serialize(JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("Name", name);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeStringField("Type", "AttachmentDELETE");
        generator.writeEndObject();
    }

}
