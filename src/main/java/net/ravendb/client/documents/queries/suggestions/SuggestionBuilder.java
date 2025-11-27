package net.ravendb.client.documents.queries.suggestions;

import net.ravendb.client.documents.conventions.DocumentConventions;

public class SuggestionBuilder<T> implements ISuggestionBuilder<T>, ISuggestionOperations<T> {

    private final DocumentConventions conventions;
    private SuggestionWithTerm _term;
    private SuggestionWithTerms _terms;

    public SuggestionBuilder(DocumentConventions conventions)
    {
        this.conventions = conventions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionOperations<T> withDisplayName(String displayName) {
        getSuggestion().setDisplayField(displayName);

        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionOperations<T> byField(String fieldName, String term) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName cannot be null");
        }

        if (term == null) {
            throw new IllegalArgumentException("term cannot be null");
        }

        _term = new SuggestionWithTerm(fieldName);
        _term.setTerm(term);

        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionOperations<T> byField(String fieldName, String[] terms) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName cannot be null");
        }

        if (terms == null) {
            throw new IllegalArgumentException("terms cannot be null");
        }

        if (terms.length == 0) {
            throw new IllegalArgumentException("Terms cannot be an empty collection.");
        }

        _terms = new SuggestionWithTerms(fieldName);
        _terms.setTerms(terms);

        return this;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ISuggestionOperations<T> withOptions(SuggestionOptions options) {
        getSuggestion().setOptions(options);

        return this;
    }

    public SuggestionBase getSuggestion() {
        if (_term != null) {
            return _term;
        }

        return _terms;
    }
}
