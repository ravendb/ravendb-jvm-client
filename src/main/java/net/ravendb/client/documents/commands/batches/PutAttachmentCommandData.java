package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.attachments.RemoteAttachmentParameters;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a command for adding (storing) an attachment to a document as part of a batch operation.
 *
 * <p>
 * This command is used during batch operations to associate files with documents. It supports both
 * standard local attachments and remote attachments that are scheduled for upload to external cloud
 * storage providers such as Amazon S3 or Azure Blob Storage.
 * </p>
 *
 * <p>
 * This class is typically used internally by the document session when saving changes that include
 * attachment operations, and it implements the {@link ICommandData} contract.
 * </p>
 */
public class PutAttachmentCommandData implements ICommandData {

    /**
     * Gets the ID of the document to which the attachment will be added.
     */
    private String id;

    /**
     * Gets the name of the attachment.
     *
     * <p>
     * The attachment name is used as a unique identifier for the attachment within the
     * context of its parent document. A single document may contain multiple attachments,
     * each distinguished by its own unique name.
     * </p>
     */
    private String name;

    /**
     * Gets the stream containing the attachment data.
     *
     * <p>
     * The stream must be seekable and have a known length for proper attachment storage.
     * The attachment data from this stream is always stored locally in the database first.
     * </p>
     *
     * <p>
     * If remote parameters are provided, the stream content is stored locally initially,
     * and a background upload process will later read it from local storage in order to
     * upload it to cloud storage.
     * </p>
     *
     * <p>
     * The stream is not closed or disposed by this class; the caller is responsible for
     * managing and closing the stream when appropriate.
     * </p>
     */
    private InputStream stream;

    /**
     * Gets the change vector for optimistic concurrency control.
     *
     * <p>
     * When specified, the attachment operation will only succeed if the document's current
     * change vector matches this value. This ensures that the document has not been modified
     * since it was last read.
     * </p>
     *
     * <p>
     * If {@code null}, no concurrency check is performed and the attachment will be added
     * regardless of the document's current state.
     * </p>
     *
     */
    private String changeVector;

    /**
     * Gets the MIME content type of the attachment.
     *
     * <p>
     * The content type indicates the nature and format of the attachment data. Common examples include:
     * </p>
     *
     * <ul>
     *     <li><code>image/jpeg</code> for JPEG images</li>
     *     <li><code>application/pdf</code> for PDF documents</li>
     *     <li><code>video/mp4</code> for MP4 videos</li>
     *     <li><code>text/plain</code> for plain text files</li>
     * </ul>
     *
     */
    private String contentType;

    /**
     * Gets the remote attachment parameters for cloud storage upload configuration.
     *
     * <p>
     * When this property is not {@code null}, the attachment is stored locally in the database
     * together with these remote parameters as metadata. A background worker monitors attachments
     * marked with remote parameters and uploads them to the configured cloud storage provider
     * (Amazon S3 or Azure Blob Storage) at the scheduled time specified in the parameters.
     * </p>
     *
     * <p>
     * The two-phase process:
     * </p>
     *
     * <ol>
     *     <li><strong>Immediate:</strong> The attachment is stored locally with remote metadata.</li>
     *     <li><strong>Background:</strong> Upload to cloud storage occurs asynchronously.</li>
     * </ol>
     *
     * <p>
     * After a successful upload, the database retains only metadata and a reference to the remote
     * location, significantly reducing database size for large attachments while still allowing
     * transparent retrieval from cloud storage.
     * </p>
     */
    private RemoteAttachmentParameters remoteParameters;

    /**
     * Gets the command type identifier.
     *
     * <p>
     * This property always returns {@code CommandType.AttachmentPUT} to identify this command
     * as an attachment put operation in batch processing.
     * </p>
     */
    private final CommandType type = CommandType.ATTACHMENT_PUT;

    boolean fromEtl;
    String hash;
    Long sizeInBytes;

    /**
     * Initializes a new instance of the {@code PutAttachmentCommandData} class for a local attachment.
     *
     * @param documentId   The ID of the document to attach the file to.
     * @param name         The name of the attachment.
     * @param stream       The stream containing the attachment data. Must be seekable and have a known length.
     * @param contentType  The MIME content type of the attachment (e.g., "image/jpeg", "application/pdf").
     * @param changeVector Optional change vector for optimistic concurrency control. If provided, the operation
     *                     will only succeed if the document's change vector matches. Pass {@code null} to skip
     *                     concurrency checks.
     *
     * <p>
     * This constructor is used for standard local attachments that will be stored directly in the database
     * without remote cloud storage. The stream must be seekable and have a known length, as these properties
     * are required for proper attachment storage.
     * </p>
     *
     * <p>
     * Stream validation is performed during construction. If the stream does not meet requirements,
     * an exception will be thrown.
     * </p>
     */
    public PutAttachmentCommandData(String documentId, String name, InputStream stream, String contentType, String changeVector) {
        this(documentId, name, stream, contentType, changeVector, null, null, null, false);
    }

    /**
     * Initializes a new instance of the {@code PutAttachmentCommandData} class for a remote attachment.
     *
     * @param documentId               The ID of the document to attach the file to.
     * @param name                     The name of the attachment.
     * @param stream                   The stream containing the attachment data. Must be seekable and have a known length.
     * @param contentType              The MIME content type of the attachment (e.g., "image/jpeg", "application/pdf").
     * @param changeVector             Optional change vector for optimistic concurrency control. If provided, the operation
     *                                 will only succeed if the document's change vector matches. Pass {@code null} to skip
     *                                 concurrency checks.
     * @param remoteAttachmentParameters
     *                                 Parameters specifying the remote storage configuration and upload schedule. When provided,
     *                                 the attachment will be stored locally first with remote metadata, then uploaded to the
     *                                 cloud storage provider by a background process.
     *
     * <p>
     * This constructor is used for attachments that should be uploaded to cloud storage
     * (Amazon S3 or Azure Blob Storage).
     * </p>
     */
    public PutAttachmentCommandData(String documentId, String name, InputStream stream, String contentType, String changeVector, RemoteAttachmentParameters remoteAttachmentParameters) {
        this(documentId, name, stream, contentType, changeVector, null, remoteAttachmentParameters, null, false);
    }


    PutAttachmentCommandData(String documentId, String name, InputStream stream, String contentType, String changeVector, Long size, RemoteAttachmentParameters remoteAttachmentParameters, String hash, boolean fromEtl) {
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("documentId");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name");
        }

        this.id = documentId;
        this.name = name;
        this.stream = stream;
        this.contentType = contentType;
        this.changeVector = changeVector;
        this.fromEtl = fromEtl;
        this.remoteParameters = remoteAttachmentParameters;
        this.hash = hash;
        this.sizeInBytes = size;

        PutAttachmentCommandHelper.tryValidateStream(stream, remoteParameters);
    }

    /**
     * @return The remote attachment parameters, or {@code null} for standard local-only attachment storage.
     */
    public RemoteAttachmentParameters getRemoteParameters() {
        return remoteParameters;
    }

    /**
     * @return The document ID.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @return The attachment name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return The stream containing the attachment content.
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * @return The change vector for concurrency control, or {@code null} to skip the check.
     */
    @Override
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * @return The MIME content type, or {@code null} if not specified.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return {@link  CommandType#ATTACHMENT_PUT}.
     */
    @Override
    public CommandType getType() {
        return type;
    }

    @Override
    public void serialize(JsonGenerator generator, DocumentConventions conventions) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Id", id);
        generator.writeStringField("Name", name);
        generator.writeStringField("ContentType", contentType);
        generator.writeStringField("ChangeVector", changeVector);
        generator.writeStringField("Type", "AttachmentPUT");
        generator.writeEndObject();
    }

    /**
     * Called before the session saves changes to allow the command to perform any
     * necessary preparation.
     *
     * <p>
     * This method is part of the {@code ICommandData} interface contract. For
     * attachment commands, no special preparation is required before saving, so
     * this method has no implementation.
     * </p>
     *
     * @param session The session that is saving changes.
     */
    @Override
    public void onBeforeSaveChanges(InMemoryDocumentSessionOperations session) {

    }
}
