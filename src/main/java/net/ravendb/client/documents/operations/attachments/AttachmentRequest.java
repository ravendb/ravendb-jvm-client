package net.ravendb.client.documents.operations.attachments;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a request to retrieve an attachment associated with a document.
 *
 * <p>This class encapsulates the necessary identifiers for an attachment operation,
 * ensuring that valid parameters are provided during instantiation.</p>
 */
public class AttachmentRequest {

    private final String _name;
    private final String _documentId;

    /**
     * Initializes a new instance of the {@link AttachmentRequest} class.
     *
     * @param documentId The ID of the document associated with the attachment.
     * @param name       The name of the attachment.
     * @throws IllegalArgumentException Thrown when {@code documentId} or {@code name} is null or empty.
     */
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
    /**
     * Gets the name of the attachment.
     *
     * @return The name of the attachment.
     */
    public String getName() {
        return _name;
    }
    /**
     * Gets the ID of the document associated with the attachment.
     *
     * @return The document ID.
     */
    public String getDocumentId() {
        return _documentId;
    }
}
