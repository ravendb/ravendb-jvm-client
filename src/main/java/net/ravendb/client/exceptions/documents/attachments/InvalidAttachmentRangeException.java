package net.ravendb.client.exceptions.documents.attachments;

import net.ravendb.client.exceptions.RavenException;

public final class InvalidAttachmentRangeException extends RavenException {

    public InvalidAttachmentRangeException() {
    }

    public InvalidAttachmentRangeException(String message) {
        super(message);
    }

    public InvalidAttachmentRangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidAttachmentRangeException throwFor(String documentId, String attachmentName, Long rangeFrom, Long rangeTo) {
        throw new InvalidAttachmentRangeException("There requested range '"
                + (rangeFrom != null ? rangeFrom : "") + "-" + (rangeTo != null ? rangeTo : "")
                + "' is invalid for '" + attachmentName + "' in document '" + documentId + "'.");
    }
}
