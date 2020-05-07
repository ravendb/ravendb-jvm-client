package net.ravendb.client.documents.operations.attachments;

import org.apache.commons.lang3.StringUtils;

public class AttachmentRequest {

    private String _name;
    private String _documentId;

    public AttachmentRequest(String documentId, String name) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null or whitespace.");
        }

        _documentId = documentId;
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public String getDocumentId() {
        return _documentId;
    }
}
