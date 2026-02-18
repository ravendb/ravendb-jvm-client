package net.ravendb.client.documents.operations.attachments;

/**
 * Represents the basic information of an attachment, including its name,
 * hash, content type, and size.
 *
 * <p>
 * This class serves as a base for more detailed attachment information,
 * providing essential properties for managing attachments within the database.
 * </p>
 */
public class AttachmentName {

    /**
     * The name of the attachment.
     */
    private String name;
    /**
     * The hash of the attachment content for integrity verification.
     */
    private String hash;
    /**
     * The MIME type of the attachment.
     */
    private String contentType;
    /**
     * The size of the attachment in bytes.
     */
    private long size;

    /**
     * Scheduling parameters for uploading the attachment to a remote (cloud) destination.
     * Optional; {@code null} if no remote upload has been requested.
     *
     * <p>
     * Set this to instruct the database to perform a remote upload of the attachment at the
     * specified time, as defined by {@code RemoteAttachmentParameters.getAt()} and identified
     * by {@code RemoteAttachmentParameters.getIdentifier()}.
     * </p>
     */
    private RemoteAttachmentParameters remoteParameters;

    public RemoteAttachmentParameters getRemoteParameters() {
        return remoteParameters;
    }

    public void setRemoteParameters(RemoteAttachmentParameters remoteParameters) {
        this.remoteParameters = remoteParameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
