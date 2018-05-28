package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class DeletePrefixedCommandData extends DeleteCommandData {
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean isPrefixed = true;

    public DeletePrefixedCommandData(String prefix) {
        super(prefix, null);
    }

    protected void serializeExtraFields(JsonGenerator generator) throws IOException {
        generator.writeBooleanField("IdPrefixed", isPrefixed);
    }
}
