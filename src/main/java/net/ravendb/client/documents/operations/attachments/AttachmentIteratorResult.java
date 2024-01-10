package net.ravendb.client.documents.operations.attachments;

import java.io.InputStream;

public class AttachmentIteratorResult {
    private final InputStream _stream;
    private final AttachmentDetails _details;

    public InputStream getStream() {
        return _stream;
    }

    public AttachmentDetails getDetails() {
        return _details;
    }

    public AttachmentIteratorResult(AttachmentDetails details, InputStream stream) {
        _details = details;
        _stream = stream;
    }
}
