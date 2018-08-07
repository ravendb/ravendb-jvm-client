package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class PutCommandDataBase<T extends JsonNode> implements ICommandData {

    private String id;
    private final String name = null;
    private String changeVector;
    private T document;
    private final CommandType type = CommandType.PUT;

    protected PutCommandDataBase(String id, String changeVector, T document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        this.id = id;
        this.changeVector = changeVector;
        this.document = document;

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

    public T getDocument() {
        return document;
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

        generator.writeFieldName("Document");
        generator.writeTree(document);

        generator.writeStringField("Type", "PUT");

        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
