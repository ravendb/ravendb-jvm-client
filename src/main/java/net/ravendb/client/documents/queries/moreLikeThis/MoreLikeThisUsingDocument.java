package net.ravendb.client.documents.queries.moreLikeThis;

/**
 * {@inheritDoc}
 */
public class MoreLikeThisUsingDocument extends MoreLikeThisBase {
    /**
     * JSON document that will be used as a base for operation.
     */
    private String documentJson;
    /**
     * {@inheritDoc}
     * @see IMoreLikeThisBuilderBase#usingDocument
     */
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
