package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.primitives.SharpEnum;

import java.io.IOException;

public class ForceRevisionCommandData implements ICommandData {

    private String _id;
    private String _name;
    private String _changeVector;

    public ForceRevisionCommandData(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getId() {
        return _id;
    }

    @Override
    public String getChangeVector() {
        return _changeVector;
    }

    @Override
    public CommandType getType() {
        return CommandType.FORCE_REVISION_CREATION;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", _id);
        generator.writeStringField("Type", SharpEnum.value(getType()));
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {

    }
}
