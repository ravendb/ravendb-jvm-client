package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.ForceRevisionStrategy;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.primitives.SharpEnum;

import java.io.IOException;

public class PutCommandDataBase<T extends JsonNode> implements ICommandData {

    private String id;
    private final String name = null;
    private String changeVector;
    private String originalChangeVector;
    private T document;
    private final CommandType type = CommandType.PUT;
    private ForceRevisionStrategy forceRevisionCreationStrategy;

    protected PutCommandDataBase(String id, String changeVector, String originalChangeVector, T document) {
        this(id, changeVector, originalChangeVector, document, ForceRevisionStrategy.NONE);
    }

    protected PutCommandDataBase(String id, String changeVector, String originalChangeVector, T document, ForceRevisionStrategy strategy) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        this.id = id;
        this.changeVector = changeVector;
        this.originalChangeVector = originalChangeVector;
        this.document = document;
        this.forceRevisionCreationStrategy = strategy;
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

    public String getOriginalChangeVector() {
        return originalChangeVector;
    }

    public T getDocument() {
        return document;
    }

    @Override
    public CommandType getType() {
        return type;
    }

    public ForceRevisionStrategy getForceRevisionCreationStrategy() {
        return forceRevisionCreationStrategy;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("ChangeVector", changeVector);
        if (originalChangeVector != null) {
            generator.writeStringField("OriginalChangeVector", originalChangeVector);
        }

        generator.writeFieldName("Document");
        generator.writeTree(document);

        generator.writeStringField("Type", "PUT");

        if (forceRevisionCreationStrategy != ForceRevisionStrategy.NONE) {
            generator.writeStringField("ForceRevisionCreationStrategy", SharpEnum.value(forceRevisionCreationStrategy));
        }
        generator.writeEndObject();
    }

    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {
    }
}
