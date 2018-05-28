package net.ravendb.client.documents.operations.attachments;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.io.InputStream;

public class CloseableAttachmentResult implements AutoCloseable {
    private final AttachmentDetails details;
    private final CloseableHttpResponse response;

    public CloseableAttachmentResult(CloseableHttpResponse response, AttachmentDetails details) {
        this.details = details;
        this.response = response;
    }

    public InputStream getData() throws IOException {
        return response.getEntity().getContent();
    }

    public AttachmentDetails getDetails() {
        return details;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(response);
    }
}
