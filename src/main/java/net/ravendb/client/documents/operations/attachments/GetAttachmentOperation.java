package net.ravendb.client.documents.operations.attachments;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.attachments.AttachmentType;
import net.ravendb.client.documents.attachments.RemoteAttachmentFlags;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.TimeUtils;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import net.ravendb.client.exceptions.documents.attachments.InvalidAttachmentRangeException;
import java.time.Instant;
import java.util.Objects;

public class GetAttachmentOperation implements IOperation<CloseableAttachmentResult> {

    private final String _documentId;
    private final String _name;
    private final AttachmentType _type;
    private final String _changeVector;
    private final Long _from;
    private final Long _to;

    /**
     * Retrieves an attachment from the database.
     * @param documentId The ID of the document associated with the attachment.
     * @param name The name of the attachment to be retrieved.
     * @param type The type of the attachment.
     * @param changeVector change vector for optimistic concurrency control. If no concurrency control require this should be set to null
     */
    public GetAttachmentOperation(String documentId, String name, AttachmentType type, String changeVector) {
        this(documentId, name, type, changeVector, null, null);
    }

    /**
     * Retrieves an attachment (or a range of it) from the database.
     * @param documentId The ID of the document associated with the attachment.
     * @param name The name of the attachment to be retrieved.
     * @param type The type of the attachment.
     * @param changeVector change vector for optimistic concurrency control. If no concurrency control require this should be set to null
     * @param from the position at which to start sending data
     * @param to the position at which to stop sending data
     */
    public GetAttachmentOperation(String documentId, String name, AttachmentType type, String changeVector, Long from, Long to) {
        _documentId = documentId;
        _name = name;
        _type = type;
        _changeVector = changeVector;
        _from = from;
        _to = to;
    }

    @Override
    public RavenCommand<CloseableAttachmentResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetAttachmentCommand(conventions, _documentId, _name, _type, _changeVector, _from, _to);
    }

    private static class GetAttachmentCommand extends RavenCommand<CloseableAttachmentResult> {
        private final DocumentConventions _conventions;
        private final String _documentId;
        private final String _name;
        private final AttachmentType _type;
        private final String _changeVector;
        private final Long _from;
        private final Long _to;

        public GetAttachmentCommand(DocumentConventions conventions, String documentId, String name, AttachmentType type, String changeVector, Long from, Long to) {
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

            _conventions = conventions;
            _documentId = documentId;
            _name = name;
            _type = type;
            _changeVector = changeVector;
            _from = from;
            _to = to;

            responseType = RavenCommandResponseType.EMPTY;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments?id="
                    + UrlUtils.escapeDataString(_documentId) + "&name=" + UrlUtils.escapeDataString(_name);

            String range = _from != null || _to != null ? createRangeHeaderValue(_from, _to) : null;

            if (_type == AttachmentType.REVISION) {
                HttpPost request = new HttpPost(url);
                if (range != null) {
                    request.setHeader(HttpHeaders.RANGE, range);
                }

                request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                    try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                        generator.writeStartObject();
                        generator.writeStringField("Type", "Revision");
                        generator.writeStringField("ChangeVector", _changeVector);
                        generator.writeEndObject();
                    }
                }, ContentType.APPLICATION_JSON, _conventions));

                return request;
            } else {
                HttpGet request = new HttpGet(url);
                if (range != null) {
                    request.setHeader(HttpHeaders.RANGE, range);
                }
                return request;
            }
        }

        private static String createRangeHeaderValue(Long from, Long to) {
            // mirrors System.Net.Http.Headers.RangeHeaderValue validation
            if (from != null && from < 0) {
                throw new IllegalArgumentException("from must be greater than or equal to 0");
            }
            if (to != null && to < 0) {
                throw new IllegalArgumentException("to must be greater than or equal to 0");
            }
            if (from != null && to != null && from > to) {
                throw new IllegalArgumentException("to must be greater than or equal to from");
            }

            return "bytes=" + (from != null ? from : "") + "-" + (to != null ? to : "");
        }

        @Override
        public void onResponseFailure(ClassicHttpResponse response) {
            if (response.getCode() == HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
                InvalidAttachmentRangeException.throwFor(_documentId, _name, _from, _to);
            }
        }

        @Override
        public ResponseDisposeHandling processResponse(HttpCache cache, ClassicHttpResponse response, String url) {
            String contentType = null;
            Header ct = response.getFirstHeader(Constants.Headers.CONTENT_TYPE);
            if (ct != null) {
                contentType = ct.getValue();
            }

            String changeVector = HttpExtensions.getEtagHeader(response);
            Header hashHeader = response.getFirstHeader(Constants.Headers.ATTACHMENT_HASH);
            String hash = hashHeader != null ? hashHeader.getValue() : null;

            long size = 0;
            Header sizeHeader = response.getFirstHeader(Constants.Headers.ATTACHMENT_SIZE);
            if (sizeHeader != null) {
                try {
                    size = Long.parseLong(sizeHeader.getValue());
                } catch (NumberFormatException e) {
                }
            }
            Header idHeader = response.getFirstHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_IDENTIFIER);
            String remoteIdentifier = null;

            if (idHeader != null) {
                remoteIdentifier = UrlUtils.unescapeDataString(idHeader.getValue());
            }

            RemoteAttachmentParameters remoteParameters = null;

            if (remoteIdentifier != null && !remoteIdentifier.isEmpty()) {

                Header atHeader = response.getFirstHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_AT);
                if (atHeader == null) {
                    throwOnMissingHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_AT);
                }

                String atValue = atHeader.getValue();
                Instant attachmentRemoteAt;
                try {
                    attachmentRemoteAt = Objects.requireNonNull(TimeUtils.parseInstant(atValue));
                } catch (Exception e) {
                    throwOnBadHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_AT, atValue);
                    return null;
                }

                Header flagsHeader = response.getFirstHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_FLAGS);
                if (flagsHeader == null) {
                    throwOnMissingHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_FLAGS);
                }

                String flagsValue = flagsHeader.getValue();
                RemoteAttachmentFlags attachmentFlags;
                try {
                    attachmentFlags = RemoteAttachmentFlags.valueOf(flagsValue.toUpperCase());
                } catch (Exception e) {
                    throwOnBadHeader(Constants.Headers.ATTACHMENT_REMOTE_PARAMETERS_FLAGS, flagsValue);
                    return null;
                }

                remoteParameters = new RemoteAttachmentParameters(remoteIdentifier, attachmentRemoteAt);
                remoteParameters.setFlags(attachmentFlags);
            }

            AttachmentDetails attachmentDetails = new AttachmentDetails();
            attachmentDetails.setContentType(contentType);
            attachmentDetails.setRemoteParameters(remoteParameters);
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

        private void throwOnMissingHeader(String header) {
            throw new IllegalStateException(
                    "Attachment remote parameters header '" + header +
                            "' is missing for attachment '" + _name +
                            "' on document '" + _documentId + "'."
            );
        }

        private void throwOnBadHeader(String header, String value) {
            throw new IllegalStateException(
                    "Attachment remote parameters header '" + header +
                            "' has invalid value '" + value +
                            "' for attachment '" + _name +
                            "' on document '" + _documentId + "'."
            );
        }
    }
}