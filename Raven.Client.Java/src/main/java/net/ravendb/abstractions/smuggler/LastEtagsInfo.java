package net.ravendb.abstractions.smuggler;

import net.ravendb.abstractions.data.Etag;

public class LastEtagsInfo {

    public LastEtagsInfo() {
        lastDocsEtag = Etag.empty();
        lastDocDeleteEtag = Etag.empty();
        lastAttachmentEtag = Etag.empty();
        lastAttachmentDeleteEtag = Etag.empty();
    }

    private Etag lastDocsEtag;
    private Etag lastDocDeleteEtag;
    private Etag lastAttachmentEtag;
    private Etag lastAttachmentDeleteEtag;

    public Etag getLastDocsEtag() {
        return lastDocsEtag;
    }

    public void setLastDocsEtag(Etag lastDocsEtag) {
        this.lastDocsEtag = lastDocsEtag;
    }

    public Etag getLastDocDeleteEtag() {
        return lastDocDeleteEtag;
    }

    public void setLastDocDeleteEtag(Etag lastDocDeleteEtag) {
        this.lastDocDeleteEtag = lastDocDeleteEtag;
    }

    public Etag getLastAttachmentEtag() {
        return lastAttachmentEtag;
    }

    public void setLastAttachmentEtag(Etag lastAttachmentEtag) {
        this.lastAttachmentEtag = lastAttachmentEtag;
    }

    public Etag getLastAttachmentDeleteEtag() {
        return lastAttachmentDeleteEtag;
    }

    public void setLastAttachmentDeleteEtag(Etag lastAttachmentDeleteEtag) {
        this.lastAttachmentDeleteEtag = lastAttachmentDeleteEtag;
    }
}
