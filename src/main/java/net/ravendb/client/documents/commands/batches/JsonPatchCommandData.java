package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.JsonPatchDocument;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.primitives.SharpEnum;

import java.io.IOException;

public final class JsonPatchCommandData implements ICommandData {

    private final String id;
    private final JsonPatchDocument jsonPatch;
    private final String name = null;
    private final String changeVector = null;

    private boolean returnDocument;
    private final CommandType type = CommandType.JSON_PATCH;

    public JsonPatchCommandData(String id, JsonPatchDocument patch) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
        this.jsonPatch = patch;
    }

    public String getId() {
        return id;
    }

    public JsonPatchDocument getJsonPatch() {
        return jsonPatch;
    }

    public String getName() {
        return name;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public boolean isReturnDocument() {
        return returnDocument;
    }

    public CommandType getType() {
        return type;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Id", id);
        generator.writeStringField("ChangeVector", changeVector);

        // Write the JsonPatch object
        generator.writeFieldName("JsonPatch");
        generator.writeStartObject();

        generator.writeFieldName("Operations");
        generator.writeStartArray();
        ObjectMapper mapper = new ObjectMapper();
        for (JsonPatchOperation op : jsonPatch.getOperations()) {
            generator.writeTree(mapper.valueToTree(op));
        }
        generator.writeEndArray();

        generator.writeEndObject(); // end JsonPatch

        generator.writeBooleanField("ReturnDocument", returnDocument);
        generator.writeStringField("Type", SharpEnum.value(getType()));

        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        returnDocument = session.isLoaded(id);
    }
}