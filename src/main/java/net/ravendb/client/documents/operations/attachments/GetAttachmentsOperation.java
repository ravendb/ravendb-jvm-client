package net.ravendb.client.documents.operations.attachments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.attachments.AttachmentType;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GetAttachmentsOperation implements IOperation<CloseableAttachmentsResult> {
    private final AttachmentType _type;
    private final List<AttachmentRequest> _attachments;

    public GetAttachmentsOperation(List<AttachmentRequest> attachments, AttachmentType type) {
        _type = type;
        _attachments = attachments;
    }

    @Override
    public RavenCommand<CloseableAttachmentsResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetAttachmentsCommand(_attachments, _type);
    }

    public static class GetAttachmentsCommand extends RavenCommand<CloseableAttachmentsResult> {
        private final AttachmentType _type;
        private final List<AttachmentRequest> _attachments;
        private final List<AttachmentDetails> _attachmentsMetadata = new ArrayList<>();

        public GetAttachmentsCommand(List<AttachmentRequest> attachments, AttachmentType type) {
            super(CloseableAttachmentsResult.class);

            _type = type;
            _attachments = attachments;
            responseType = RavenCommandResponseType.EMPTY;
        }

        public List<AttachmentRequest> getAttachments() {
            return _attachments;
        }

        public List<AttachmentDetails> getAttachmentsMetadata() {
            return _attachmentsMetadata;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments/bulk";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();

                    generator.writeStringField("AttachmentType", SharpEnum.value(_type));

                    generator.writeFieldName("Attachments");

                    generator.writeStartArray();
                    for (AttachmentRequest attachment : _attachments) {
                        generator.writeStartObject();
                        generator.writeStringField("DocumentId", attachment.getDocumentId());
                        generator.writeStringField("Name", attachment.getName());
                        generator.writeEndObject();
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
            try {
                InputStream stream = response.getEntity().getContent();

                try (JsonParser parser = mapper.getFactory().createParser(stream)) {
                    parser.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);

                    TreeNode responseRoot = mapper.readTree(parser);
                    TreeNode attachmentsMetadata = responseRoot.get("AttachmentsMetadata");
                    if (attachmentsMetadata instanceof ArrayNode) {
                        ArrayNode array = (ArrayNode) attachmentsMetadata;
                        for (JsonNode jsonNode : array) {
                            AttachmentDetails attachmentDetails = mapper.convertValue(jsonNode, AttachmentDetails.class);
                            _attachmentsMetadata.add(attachmentDetails);
                        }
                    }

                    ByteArrayOutputStream releasedBuffer = new ByteArrayOutputStream();
                    parser.releaseBuffered(releasedBuffer);
                    parser.close();

                    InputStream joinedStream = new SequenceInputStream(new ByteArrayInputStream(releasedBuffer.toByteArray()), stream);

                    result = new CloseableAttachmentsResult(joinedStream, _attachmentsMetadata);
                }
            } catch (IOException | UnsupportedOperationException e) {
                throwInvalidResponse();
            }

            return ResponseDisposeHandling.MANUALLY;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}