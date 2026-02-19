package net.ravendb.client.documents.operations.attachments;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an operation to delete multiple attachments from the database
 * in a single request.
 *
 * <p>
 * This operation can be used to efficiently remove multiple attachments
 * associated with documents by sending a bulk delete request to the server.
 * </p>
 */
public final class DeleteAttachmentsOperation implements IVoidOperation {

    private final Iterable<AttachmentRequest> attachments;

    /**
     * Initializes a new instance of the {@code DeleteAttachmentsOperation} class.
     *
     * <p>
     * Use this constructor to create an operation that deletes multiple attachments
     * in a single bulk request. Each {@code AttachmentRequest} should specify the
     * document ID and attachment name.
     * </p>
     *
     * @param attachments A collection of attachment requests specifying which attachments to delete.
     */
    public DeleteAttachmentsOperation(Iterable<AttachmentRequest> attachments) {
        this.attachments = attachments;
    }

    @Override
    public VoidRavenCommand getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new DeleteAttachmentsCommand(conventions, attachments);
    }

    public static final class DeleteAttachmentsCommand extends VoidRavenCommand {

        private final DocumentConventions conventions;
        final Iterable<AttachmentRequest> attachments;
        final List<AttachmentDetails> attachmentsMetadata = new ArrayList<>();

        public DeleteAttachmentsCommand(DocumentConventions conventions, Iterable<AttachmentRequest> attachments) {

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            this.conventions = conventions;
            this.attachments = attachments;
            this.responseType = RavenCommandResponseType.EMPTY;
        }

        /**
         * Builds the URL for the bulk attachment delete endpoint.
         *
         * @param node The server node.
         * @return The request URL.
         */
        public String getUrl(ServerNode node) {
            return node.getUrl() + "/databases/" + node.getDatabase() + "/attachments/bulk";
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String requestUrl = getUrl(node);

            HttpDelete request = new HttpDelete(requestUrl);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Attachments");
                    generator.writeStartArray();
//TODO: check here the json creation
                    for (AttachmentRequest attachment : attachments) {
                        generator.writeStartObject();
                        generator.writeStringField("DocumentId", attachment.getDocumentId());
                        generator.writeStringField("Name", attachment.getName());
                        generator.writeEndObject();
                    }

                    generator.writeEndArray();
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, conventions));
            return request;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}