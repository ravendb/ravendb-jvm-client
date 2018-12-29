package net.ravendb.client.documents.operations.attachments;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.attachments.AttachmentType;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class GetAttachmentOperation implements IOperation<CloseableAttachmentResult> {

    private final String _documentId;
    private final String _name;
    private final AttachmentType _type;
    private final String _changeVector;

    public GetAttachmentOperation(String documentId, String name, AttachmentType type, String changeVector) {
        _documentId = documentId;
        _name = name;
        _type = type;
        _changeVector = changeVector;
    }

    @Override
    public RavenCommand<CloseableAttachmentResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetAttachmentCommand(_documentId, _name, _type, _changeVector);
    }

    private static class GetAttachmentCommand extends RavenCommand<CloseableAttachmentResult> {
        private final String _documentId;
        private final String _name;
        private final AttachmentType _type;
        private final String _changeVector;

        public GetAttachmentCommand(String documentId, String name, AttachmentType type, String changeVector) {
            super(CloseableAttachmentResult.class);

            if (StringUtils.isBlank(documentId)) {
                throw new IllegalArgumentException("DocumentId cannot be null or empty");
            }

            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }

            if (type != AttachmentType.DOCUMENT && changeVector == null) {
                throw new IllegalArgumentException("Change vector cannot be null for attachment type " + type);
            }

            _documentId = documentId;
            _name = name;
            _type = type;
            _changeVector = changeVector;

            responseType = RavenCommandResponseType.EMPTY;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments?id="
                    + UrlUtils.escapeDataString(_documentId) + "&name=" + UrlUtils.escapeDataString(_name);

            if (_type == AttachmentType.REVISION) {
                HttpPost request = new HttpPost();

                request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                    try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                        generator.writeStartObject();
                        generator.writeStringField("Type", "Revision");
                        generator.writeStringField("ChangeVector", _changeVector);
                        generator.writeEndObject();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, ContentType.APPLICATION_JSON));

                return request;
            } else {
                return new HttpGet();
            }
        }

        @Override
        public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
            String contentType = response.getEntity().getContentType().getValue();
            String changeVector = HttpExtensions.getEtagHeader(response);
            String hash = response.getFirstHeader("Attachment-Hash").getValue();
            long size = 0;

            Header sizeHeader = response.getFirstHeader("Attachment-Size");
            if (sizeHeader != null) {
                size = Long.valueOf(sizeHeader.getValue());
            }

            AttachmentDetails attachmentDetails = new AttachmentDetails();
            attachmentDetails.setContentType(contentType);
            attachmentDetails.setName(_name);
            attachmentDetails.setHash(hash);
            attachmentDetails.setSize(size);
            attachmentDetails.setChangeVector(changeVector);
            attachmentDetails.setDocumentId(_documentId);

            result = new CloseableAttachmentResult(response, attachmentDetails);

            return ResponseDisposeHandling.MANUALLY;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

    }
}
