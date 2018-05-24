package net.ravendb.client.documents.queries.suggestions;

public class SuggestionBuilder<T> implements ISuggestionBuilder<T>, ISuggestionOperations<T> {

    private SuggestionWithTerm _term;
    private SuggestionWithTerms _terms;

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
