package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.io.IOException;

public interface ICommandData {
    String getId();

    String getName();

    String getChangeVector();

    CommandType getType();

    void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException;

    void onBeforeSaveChanges(InMemoryDocumentSessionOperations session);
}
