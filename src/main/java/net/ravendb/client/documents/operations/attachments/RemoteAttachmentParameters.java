package net.ravendb.client.documents.operations.attachments;

import net.ravendb.client.documents.attachments.RemoteAttachmentFlags;
import java.time.Instant;

/**
 * Represents the scheduling parameters for uploading an attachment to remote storage in RavenDB.
 *
 * <p>
 * This type is used when instructing RavenDB to perform an upload to remote cloud storage of an attachment
 * at a specified time. The {@link #getAt()} value should normally be expressed in UTC.
 * </p>
 *
 * <pre>
 * Example:
 * RemoteAttachmentParameters p =
 *     new RemoteAttachmentParameters("s3-storage", Instant.now().plusSeconds(5));
 * </pre>
 */
public class RemoteAttachmentParameters {

    /**
     * Initializes a new instance of the {@code RemoteAttachmentParameters} class.
     * Parameterless constructor for serialization purposes.
     */
    public RemoteAttachmentParameters() {
        // Parameterless constructor for serialization
    }

    /**
     * Initializes a new instance of the {@code RemoteAttachmentParameters} class with the specified
     * destination identifier and scheduled remote upload time.
     *
     * @param identifier
     *     A unique identifier specifying the remote destination for uploading the attachment.
     *
     * @param at
     *     The (usually UTC) date and time at which the remote upload should be executed.
     *     Must not be the default {@link Instant} value.
     *
     * @throws IllegalArgumentException
     *     If {@code identifier} is null or blank.
     *
     * @throws IllegalArgumentException
     *     If {@code at} is the default {@link Instant} value.
     */
    public RemoteAttachmentParameters(String identifier, Instant at) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment identifier cannot be null or whitespace.");
        }
        if (at == null) {
            throw new IllegalArgumentException("Attachment upload date cannot be default value.");
        }

        this.identifier = identifier;
        this.at = at;
    }

    /**
     * Gets or sets the scheduled (preferably UTC) date and time when the attachment should be uploaded
     * to the remote destination.
     */
    private Instant at;

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }

    /**
     * Gets or sets the identifier of the remote storage destination to which the attachment should be uploaded.
     */
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets or sets flags controlling the remote upload behavior.
     * Use {@link RemoteAttachmentFlags#REMOTE} to mark the attachment for remote handling.
     */
    RemoteAttachmentFlags flags;

    public RemoteAttachmentFlags getFlags() {
        return flags;
    }

    public void setFlags(RemoteAttachmentFlags flags) {
        this.flags = flags;
    }
}
