package net.ravendb.client.exceptions.documents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.exceptions.ConflictException;

public class DocumentConflictException extends ConflictException {

    private String docId;
    private long largestEtag;

    public DocumentConflictException() {
    }

    public DocumentConflictException(String message, String docId, long etag) {
        super(message);
        this.docId = docId;
        this.largestEtag = etag;
    }

    public static DocumentConflictException fromMessage(String message) {
        return new DocumentConflictException(message, null, 0);
    }

    public static DocumentConflictException fromJson(ObjectNode json) {
        JsonNode docId = json.get("DocId");
        JsonNode message = json.get("Message");
        JsonNode largestEtag = json.get("LargestEtag");

        return new DocumentConflictException(message.asText(), docId.asText(), largestEtag.asLong());
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public long getLargestEtag() {
        return largestEtag;
    }

    public void setLargestEtag(long largestEtag) {
        this.largestEtag = largestEtag;
    }
}
