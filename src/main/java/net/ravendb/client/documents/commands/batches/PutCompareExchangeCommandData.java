package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class PutCompareExchangeCommandData implements ICommandData {

    private final long index;
    private final ObjectNode document;

    private String id;
    private String name;
    private String changeVector;

    public PutCompareExchangeCommandData(String key, ObjectNode value, long index) {
        id = key;
        document = value;
        this.index = index;
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
        return CommandType.COMPARE_EXCHANGE_PUT;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeObjectField("Document", document);
        generator.writeNumberField("Index", index);
        generator.writeStringField("Type", "CompareExchangePUT");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
