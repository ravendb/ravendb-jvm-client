package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public class PatchCommandData implements ICommandData {

    private String id;
    private String name;
    private String changeVector;
    private PatchRequest patch;
    private PatchRequest patchIfMissing;
    private boolean returnDocument;

    public PatchCommandData(String id, String changeVector, PatchRequest patch, PatchRequest patchIfMissing) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (patch == null) {
            throw new IllegalArgumentException("Patch cannot be null");
        }
        this.id = id;
        this.patch = patch;
        this.changeVector = changeVector;
        this.patchIfMissing = patchIfMissing;
    }

    public boolean isReturnDocument() {
        return returnDocument;
    }

    public void setReturnDocument(boolean returnDocument) {
        this.returnDocument = returnDocument;
    }

    @Override
    public CommandType getType() {
        return CommandType.PATCH;
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

    public PatchRequest getPatch() {
        return patch;
    }

    public PatchRequest getPatchIfMissing() {
        return patchIfMissing;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("Id", id);
        generator.writeStringField("ChangeVector", changeVector);

        generator.writeFieldName("Patch");
        patch.serialize(generator, conventions.getEntityMapper());
        generator.writeObjectField("Type", "PATCH");

        if (patchIfMissing != null) {
            generator.writeFieldName("PatchIfMissing");
            patchIfMissing.serialize(generator, conventions.getEntityMapper());
        }

        if (returnDocument) {
            generator.writeBooleanField("ReturnDocument", returnDocument);
        }

        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
        returnDocument = session.isLoaded(getId());
    }
}
