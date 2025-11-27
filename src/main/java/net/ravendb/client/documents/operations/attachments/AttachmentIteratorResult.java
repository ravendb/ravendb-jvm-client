package net.ravendb.client.documents.operations.attachments;

import java.io.InputStream;

/**
 * Represents the result of an attachment enumeration operation, containing the attachment stream and its details.
 *
 * <p>This class encapsulates the metadata and the binary content of an attachment retrieved during enumeration.</p>
 */
public class AttachmentIteratorResult {
    private final InputStream _stream;
    private final AttachmentDetails _details;

    /**
     * The stream containing the binary content of the attachment.
     * @return stream
     */
    public InputStream getStream() {
        return _stream;
    }

    /**
     * Gets The details of the attachment, including its metadata.
     * @return attachment details
     */
    public AttachmentDetails getDetails() {
        return _details;
    }

    public AttachmentIteratorResult(AttachmentDetails details, InputStream stream) {
        _details = details;
        _stream = stream;
    }
}
