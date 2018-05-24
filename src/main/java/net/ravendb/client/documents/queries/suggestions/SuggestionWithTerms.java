package net.ravendb.client.documents.queries.suggestions;

public class SuggestionWithTerms extends SuggestionBase {

    private String[] terms;

    public SuggestionWithTerms(String field) {
        super(field);
    }

    public String[] getTerms() {
        return terms;
    }

    public void setTerms(String[] terms) {
        this.terms = terms;
    }
}