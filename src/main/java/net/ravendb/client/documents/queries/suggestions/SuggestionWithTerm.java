package net.ravendb.client.documents.queries.suggestions;

public class SuggestionWithTerm extends SuggestionBase {
    private String term;

    public SuggestionWithTerm(String field) {
        super(field);
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
