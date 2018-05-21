package net.ravendb.client.documents.queries.moreLikeThis;

public class MoreLikeThisUsingDocument extends MoreLikeThisBase {

    private String documentJson;

    public MoreLikeThisUsingDocument(String documentJson) {
        this.documentJson  = documentJson;
    }

    public String getDocumentJson() {
        return documentJson;
    }

    public void setDocumentJson(String documentJson) {
        this.documentJson = documentJson;
    }
}
