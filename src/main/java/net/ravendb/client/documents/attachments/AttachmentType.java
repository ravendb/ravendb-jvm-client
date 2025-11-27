package net.ravendb.client.documents.attachments;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Specifies the types of attachments that can be retrieved or managed in the database.
 * <p>
 * This enumeration differentiates between document attachments and revision attachments.
 */
@UseSharpEnum
public enum AttachmentType {
    /**
     * Indicates that the attachment is associated with a document.
     */
    DOCUMENT,
    /**
     * Indicates that the attachment is associated with a revision.
     */
    REVISION
}
