package net.ravendb.client.documents.queries;

import java.util.Set;

public class TermsQueryResult {
    private Set<String> terms;
    private long resultEtag;
    private String indexName;

    public Set<String> getTerms() {
        return terms;
    }

    public void setTerms(Set<String> terms) {
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
