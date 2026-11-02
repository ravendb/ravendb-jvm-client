package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class JsonPatchCommandData implements ICommandData {

    private final String id;
    private final JsonPatchDocument jsonPatch;
    private boolean returnDocument;

    public JsonPatchCommandData(String id, JsonPatchDocument jsonPatch) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        this.id = id;
        this.jsonPatch = jsonPatch;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getChangeVector() {
        return null;
    }

    @Override
    public CommandType getType() {
        return CommandType.JSON_PATCH;
    }

    public JsonPatchDocument getJsonPatch() {
        return jsonPatch;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Id", id);
        generator.writeNullField("ChangeVector");

        generator.writeFieldName("JsonPatch");
        jsonPatch.serialize(generator, conventions.getEntityMapper());

        generator.writeBooleanField("ReturnDocument", returnDocument);
        generator.writeStringField("Type", "JsonPatch");

        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        returnDocument = session.isLoaded(id);
    }
}
