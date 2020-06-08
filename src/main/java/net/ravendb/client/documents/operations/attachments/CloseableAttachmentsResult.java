package net.ravendb.client.documents.operations.attachments;

import net.ravendb.client.exceptions.RavenException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CloseableAttachmentsResult implements AutoCloseable, Iterator<AttachmentIteratorResult> {

    private int idx = 0;
    private final InputStream _stream;
    private final List<AttachmentDetails> _attachmentsMetadata;
    private LimitedInputStream previousStream;

    public CloseableAttachmentsResult(InputStream stream, List<AttachmentDetails> attachmentsMetadata) {
        _stream = stream;
        _attachmentsMetadata = attachmentsMetadata;
    }

    @Override
    public boolean hasNext() {
        return idx < _attachmentsMetadata.size();
    }

    @Override
    public AttachmentIteratorResult next() {
        if (previousStream != null) {
            try {
                previousStream.close();
            } catch (IOException e) {
                throw new RavenException("Unable to move to next attachment");
            }
        }

        if (idx >= _attachmentsMetadata.size()) {
            throw new NoSuchElementException();
        }

        //TODO: do we want to introduce buffer here?

        AttachmentDetails currentAttachment = _attachmentsMetadata.get(idx);
        long attachmentSize = currentAttachment.getSize();
        previousStream = new LimitedInputStream(_stream, attachmentSize);

        idx++;

        return new AttachmentIteratorResult(currentAttachment, previousStream);
    }

    @Override
    public void close() throws Exception {
        if (previousStream != null) {
            previousStream.close();
        }
        _stream.close();
    }
}
