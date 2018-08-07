package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class DeleteCommandData implements ICommandData {

    private final String id;
    private String name;
    private String changeVector;
    private final CommandType type = CommandType.DELETE;

    public DeleteCommandData(String id, String changeVector) {
        this.id = id;
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
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

    @Override
    public String getChangeVector() {
        return changeVector;
    }

    @Override
    public CommandType getType() {
        return type;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Id", id);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeObjectField("Type", "DELETE");

        serializeExtraFields(generator);

        generator.writeEndObject();
    }

    protected void serializeExtraFields(JsonGenerator generator) throws IOException {
        // empty by design
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
