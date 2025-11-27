package net.ravendb.client.documents.queries.suggestions;

/**
 * {@inheritDoc}
 */
public final class SuggestionWithTerms extends SuggestionBase {
    /**
     * List of terms for which to get suggested similar terms
     */
    private String[] terms;
    /**
     * {@inheritDoc}
     */
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