package net.ravendb.client.documents.operations.attachments;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.TimeUtils;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class PutAttachmentOperation implements IOperation<AttachmentDetails> {
    private final String _documentId;
    private final String _name;
    private final InputStream _stream;
    private final String _contentType;
    private final RemoteAttachmentParameters remoteParameters;
    private final String _changeVector;

    public PutAttachmentOperation(String documentId, String name, InputStream stream) {
        this(documentId, name, stream, null, null, null);
    }

    public PutAttachmentOperation(String documentId, String name, InputStream stream, String contentType) {
        this(documentId, name, stream, contentType, null, null);
    }

    public PutAttachmentOperation(String documentId, String name, InputStream stream, String contentType, RemoteAttachmentParameters remoteParameters, String changeVector) {
        _documentId = documentId;
        _name = name;
        _stream = stream;
        _contentType = contentType;
        _changeVector = changeVector;
        this.remoteParameters = remoteParameters;
    }

    public PutAttachmentOperation(String documentId, StoreAttachmentParameters parameters) {
        _documentId = documentId;
        _name = parameters.getName();
        _stream = new ByteArrayInputStream(parameters.getBytes());
        _contentType = parameters.getContentType();
        this.remoteParameters = parameters.getRemoteParameters();
        _changeVector = parameters.getChangeVector();
    }

    @Override
    public RavenCommand<AttachmentDetails> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new PutAttachmentCommand(conventions,_documentId, _name, _stream, _contentType, _changeVector, this.remoteParameters);
    }

    private static class PutAttachmentCommand extends RavenCommand<AttachmentDetails> {
        private final DocumentConventions _conventions;
        private final String _documentId;
        private final String _name;
        private final InputStream _stream;
        private final String _contentType;
        private final RemoteAttachmentParameters remoteParameters;
        private final String _changeVector;

        public PutAttachmentCommand(DocumentConventions conventions,String documentId, String name, InputStream stream, String contentType, String changeVector, RemoteAttachmentParameters remoteParameters) {
            super(AttachmentDetails.class);

            if (StringUtils.isBlank(documentId)) {
                throw new IllegalArgumentException("documentId");
            }
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("name");
            }

            this._conventions = conventions;
            this._documentId = documentId;
            this._name = name;
            this._stream = stream;
            this._contentType = contentType;
            this.remoteParameters = remoteParameters;
            this._changeVector = changeVector;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/attachments?id=" + UrlUtils.escapeDataString(_documentId) + "&name=" + UrlUtils.escapeDataString(_name);

            if (StringUtils.isNotEmpty(_contentType)) {
                url += "&contentType=" + UrlUtils.escapeDataString(_contentType);
            }

            if (remoteParameters != null) {
                ZonedDateTime at = remoteParameters.getAt().atZone(ZoneOffset.UTC);
                try{
                    url += "&remoteAt=" + URLEncoder.encode(at.format(TimeUtils.RAVEN_FORMAT), "UTF-8");

                    url += "&remoteIdentifier=" + URLEncoder.encode(
                            remoteParameters.getIdentifier(),
                            "UTF-8"
                    );
                }catch (UnsupportedEncodingException ex){
                    throw new RuntimeException("Failed to encode remote attachment parameters.", ex);
                }
            }

            HttpPut request = new HttpPut(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                IOUtils.copy(_stream, outputStream);
            }, null, _conventions));

            addChangeVectorIfNotNull(_changeVector, request);
            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, AttachmentDetails.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
