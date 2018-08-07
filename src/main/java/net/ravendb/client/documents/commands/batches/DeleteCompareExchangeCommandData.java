package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class DeleteCompareExchangeCommandData implements ICommandData {

    public final long index;

    private String id;
    private String name;
    private String changeVector;

    public DeleteCompareExchangeCommandData(String key, long index) {
        this.id = key;
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
        return CommandType.COMPARE_EXCHANGE_DELETE;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeNumberField("Index", index);
        generator.writeStringField("Type", "CompareExchangeDELETE");
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
