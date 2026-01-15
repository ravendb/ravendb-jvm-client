package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.List;

/**
 * Represents counter operations for a specific document, such as increment or set operations on named counters.
 */
public class DocumentCountersOperation {

    /**
     * A list of counter operations to be performed on the specified document.
     * Each operation in the list specifies an action, such as incrementing or deleting a counter.
     *
     * <p><strong>Remarks:</strong> The {@code operations} field is mandatory and must be populated
     * before the batch operation is executed. If it is not set or is empty, an exception will be
     * thrown during parsing or execution.</p>
     */
    private List<CounterOperation> operations;
    /**
     * The ID of the document on which the counter operations are to be performed.
     *
     * <p><strong>Remarks:</strong> The {@code documentId} field is mandatory and identifies
     * the target document for the counter operations. If it is not set, an exception will be
     * thrown during parsing or execution.</p>
     */
    private String documentId;

    public void serialize(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("DocumentId", documentId);

        generator.writeFieldName("Operations");
        generator.writeStartArray();
        for (CounterOperation operation : operations) {
            operation.serialize(generator);
        }
        generator.writeEndArray();

        generator.writeEndObject();
    }

    public List<CounterOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<CounterOperation> operations) {
        this.operations = operations;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
