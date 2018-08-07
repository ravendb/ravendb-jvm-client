package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CounterBatch {

    private boolean replyWithAllNodesValues;
    private List<DocumentCountersOperation> documents = new ArrayList<>();
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

    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();

        generator.writeBooleanField("ReplyWithAllNodesValues", replyWithAllNodesValues);
        generator.writeFieldName("Documents");
        generator.writeStartArray();

        for (DocumentCountersOperation documentCountersOperation : documents) {
            documentCountersOperation.serialize(generator, conventions);
        }

        generator.writeEndArray();
        generator.writeBooleanField("FromEtl", fromEtl);
        generator.writeEndObject();
    }
}
