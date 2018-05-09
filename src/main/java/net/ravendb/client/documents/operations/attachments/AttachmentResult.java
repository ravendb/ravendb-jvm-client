package net.ravendb.client.documents.operations.attachments;

import java.io.InputStream;

public class AttachmentResult {
    private InputStream data;
    private AttachmentDetails details;

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public AttachmentDetails getDetails() {
        return details;
    }

    public void setDetails(AttachmentDetails details) {
        this.details = details;
    }
}
