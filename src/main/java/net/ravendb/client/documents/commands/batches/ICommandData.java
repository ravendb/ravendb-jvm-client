package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public interface ICommandData {
    String getId();

    String getName();

    String getChangeVector();

    CommandType getType();

    void serialize(JsonGenerator generator, SerializerProvider serializerProvider) throws IOException;
}
