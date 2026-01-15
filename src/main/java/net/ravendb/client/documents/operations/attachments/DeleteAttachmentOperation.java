package net.ravendb.client.documents.operations.attachments;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Represents an operation to delete an attachment from the database.
 *
 * <p>This operation can be used to remove an existing attachment associated with a document.</p>
 */
public class DeleteAttachmentOperation implements IVoidOperation {

    private final String _documentId;
    private final String _name;
    private final String _changeVector;


    public DeleteAttachmentOperation(String documentId, String name) {
        this(documentId, name, null);
    }

    /**
     * Initializes a new instance of the {@link DeleteAttachmentOperation} class.
     *
     * @param documentId   The ID of the document from which the attachment will be deleted.
     * @param name         The name of the attachment to be deleted.
     * @param changeVector An optional change vector of the attachment for optimistic concurrency control.
     *
     * <p>Use this constructor to create an operation that deletes an attachment
     * associated with the specified document. If the {@code changeVector} is provided,
     * it ensures that the attachment is only deleted if the specified version of the attachment is the current one.</p>
     */
    public DeleteAttachmentOperation(String documentId, String name, String changeVector) {
        _documentId = documentId;
        _name = name;
        _changeVector = changeVector;
    }

    @Override
    public VoidRavenCommand getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new DeleteAttachmentCommand(_documentId, _name, _changeVector);
    }

    private static class DeleteAttachmentCommand extends VoidRavenCommand {
        private final String _documentId;
        private final String _name;
        private final String _changeVector;

        public DeleteAttachmentCommand(String documentId, String name, String changeVector) {
            if (StringUtils.isBlank(documentId)) {
                throw new IllegalArgumentException("documentId cannot be null");
            }

            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("name cannot be null");
            }

            _documentId = documentId;
            _name = name;
            _changeVector = changeVector;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments?id=" + UrlUtils.escapeDataString(_documentId) + "&name=" + UrlUtils.escapeDataString(_name);

            HttpDelete request = new HttpDelete(url);

            addChangeVectorIfNotNull(_changeVector, request);
            return request;
        }
    }
}
