package net.ravendb.client.documents.attachments;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Flags that indicate the location and characteristics of an attachment.
 */
@UseSharpEnum
public enum RemoteAttachmentFlags {

    /**
     * No flags are set. The attachment is stored locally.
     */
    NONE(0),

    /**
     * The attachment is stored remotely in cloud storage rather than in the local database.
     */
    REMOTE(0x1);

    private final int value;

    RemoteAttachmentFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
