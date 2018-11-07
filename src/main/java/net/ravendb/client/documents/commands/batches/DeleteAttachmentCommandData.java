package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class DeleteAttachmentCommandData implements ICommandData {

    private String id;
    private String name;
    private String changeVector;
    private final CommandType type = CommandType.ATTACHMENT_DELETE;

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
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        this.id = documentId;
        this.name = name;
        this.changeVector = changeVector;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("Name", name);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeStringField("Type", "AttachmentDELETE");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
