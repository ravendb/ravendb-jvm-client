package net.ravendb.client.documents.queries;

import java.util.List;

public class TermsQueryResult {
    private List<String> terms;
    private long resultEtag;
    private String indexName;

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public long getResultEtag() {
        return resultEtag;
    }

    public void setResultEtag(long resultEtag) {
        this.resultEtag = resultEtag;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
