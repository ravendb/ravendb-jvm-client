package net.ravendb.client.documents.queries.suggestions;

/**
 * {@inheritDoc}
 */
public final class SuggestionWithTerm extends SuggestionBase {
    /**
     * The term for which to get suggested similar terms.
     */
    private String term;
    /**
     * {@inheritDoc}
     */
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
