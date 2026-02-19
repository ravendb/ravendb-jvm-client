package net.ravendb.client.documents.operations.attachments;

import java.io.InputStream;

public class AttachmentIteratorResult implements AutoCloseable{
    private InputStream _stream;
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

    @Override
    public void close() throws Exception {
        _stream.close();
        _stream = null;
    }
}
