package net.ravendb.client.documents.operations.attachments;

import java.io.InputStream;

/**
 * The parameters for storing an attachment in the database.
 */
public class StoreAttachmentParameters implements IStoreAttachmentParameters {

    /**
     * {@inheritDoc}
     */
    private String name;

    /**
     * {@inheritDoc}
     */
    private InputStream stream;

    /**
     * {@inheritDoc}
     */
    private String changeVector;

    /**
     * {@inheritDoc}
     */
    private String contentType;

    /**
     * {@inheritDoc}
     */
    private RemoteAttachmentParameters remoteParameters;

    /**
     * Initializes a new instance of the {@code StoreAttachmentParameters} class.
     *
     * @param name
     *     The name of the attachment to store. Cannot be null or whitespace.
     *
     * @param stream
     *     The stream containing the attachment data. Cannot be null.
     *
     * @throws IllegalArgumentException
     *     Thrown when {@code name} is null or whitespace, or when {@code stream} is null.
     *
     * <p>
     * Use this constructor to create parameters for storing an attachment with the specified name and stream.
     * Optional properties such as {@link #getChangeVector()}, {@link #getContentType()}, and
     * {@link #getRemoteParameters()} can be set after construction to provide additional control over the
     * attachment storage behavior.
     * </p>
     */
    public StoreAttachmentParameters(String name, InputStream stream, String contentType) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment name cannot be null or whitespace.");
        }
        if (stream == null) {
            throw new IllegalArgumentException("Attachment stream cannot be null.");
        }

        this.name = name;
        this.stream = stream;
        this.contentType = contentType;
    }

    public StoreAttachmentParameters(String name , InputStream stream) {
        this(name, stream, null);
    }

    /**
     * Parameterless constructor for serialization.
     */
    StoreAttachmentParameters() {
        // Parameterless constructor for serialization
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    @Override
    public String getChangeVector() {
        return changeVector;
    }

    @Override
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public RemoteAttachmentParameters getRemoteParameters() {
        return remoteParameters;
    }

    @Override
    public void setRemoteParameters(RemoteAttachmentParameters remoteParameters) {
        this.remoteParameters = remoteParameters;
    }
}
