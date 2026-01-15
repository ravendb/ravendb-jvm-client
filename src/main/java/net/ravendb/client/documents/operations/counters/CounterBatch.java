package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batch of counter operations on multiple documents, supporting retrieval of counter values across nodes.
 */
public class CounterBatch {

    /**
     * A value indicating whether the response should include counter values from all nodes.
     * When set to {@code true}, the response includes the values of each counter from all nodes in the cluster.
     */
    private boolean replyWithAllNodesValues;
    /**
     * Gets or sets the list of counter operations to be performed on the specified documents.
     * Each {@link DocumentCountersOperation} represents a set of counter operations for a single document.
     */
    private List<DocumentCountersOperation> documents = new ArrayList<>();
    /**
     * Gets or sets a value indicating whether the batch originated from an ETL process.
     * This is used internally to identify and manage counter operations triggered by ETL pipelines.
     */
    private boolean fromEtl;

    public boolean isReplyWithAllNodesValues() {
        return replyWithAllNodesValues;
    }

    public void setReplyWithAllNodesValues(boolean replyWithAllNodesValues) {
        this.replyWithAllNodesValues = replyWithAllNodesValues;
    }

    public List<DocumentCountersOperation> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentCountersOperation> document) {
        this.documents = document;
    }

    public boolean isFromEtl() {
        return fromEtl;
    }

    public void setFromEtl(boolean fromEtl) {
        this.fromEtl = fromEtl;
    }

    public void serialize(JsonGenerator generator) throws IOException {
        generator.writeStartObject();

        generator.writeBooleanField("ReplyWithAllNodesValues", replyWithAllNodesValues);
        generator.writeFieldName("Documents");
        generator.writeStartArray();

        for (DocumentCountersOperation documentCountersOperation : documents) {
            documentCountersOperation.serialize(generator);
        }

        generator.writeEndArray();
        generator.writeBooleanField("FromEtl", fromEtl);
        generator.writeEndObject();
    }
}
