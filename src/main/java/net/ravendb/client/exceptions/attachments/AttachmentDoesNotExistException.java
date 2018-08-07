package net.ravendb.client.exceptions.attachments;

import net.ravendb.client.exceptions.RavenException;

public class AttachmentDoesNotExistException extends RavenException {
    public AttachmentDoesNotExistException() {
    }

    public AttachmentDoesNotExistException(String message) {
        super(message);
    }

    public AttachmentDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AttachmentDoesNotExistException throwFor(String documentId, String attachmentName) {
        throw new AttachmentDoesNotExistException("There is no attachment with '" + attachmentName + "' name for document '" + documentId + "'");
    }
}
