package net.ravendb.client.exceptions.documents;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.exceptions.BadResponseException;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;

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

    public static DocumentConflictException fromJson(String json) {
        try {
            JsonNode jsonNode = JsonExtensions.getDefaultMapper().readTree(json);
            JsonNode docId = jsonNode.get("DocId");
            JsonNode message = jsonNode.get("Message");
            JsonNode largestEtag = jsonNode.get("LargestEtag");

            return new DocumentConflictException(message.asText(), docId.asText(), largestEtag.asLong());
        } catch (IOException e) {
            throw new BadResponseException("Unable to parse server response: ", e);
        }
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
