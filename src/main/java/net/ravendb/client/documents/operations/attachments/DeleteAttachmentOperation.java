package net.ravendb.client.documents.operations.attachments;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteAttachmentOperation implements IVoidOperation {

    private final String _documentId;
    private final String _name;
    private final String _changeVector;

    public DeleteAttachmentOperation(String documentId, String name) {
        this(documentId, name, null);
    }

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments?id=" + UrlUtils.escapeDataString(_documentId) + "&name=" + UrlUtils.escapeDataString(_name);

            HttpDelete request = new HttpDelete();

            addChangeVectorIfNotNull(_changeVector, request);
            return request;
        }
    }
}
