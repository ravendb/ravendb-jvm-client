package net.ravendb.client.documents.operations.attachments;

public class AttachmentNameWithCount extends AttachmentName {
    private long count;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
