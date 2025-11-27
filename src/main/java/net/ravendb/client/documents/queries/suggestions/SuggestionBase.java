package net.ravendb.client.documents.queries.suggestions;

import net.ravendb.client.DocumentationUrls;

/**
 * Given a string term (or terms), the Suggestion feature will offer similar terms from your data.
 * Word similarities are found using string distance algorithms.
 * {@inheritDoc}
 * @see DocumentationUrls.Session.Querying#SuggestionsQuery
 */
public abstract class SuggestionBase {
    /**
     * Field on which perform term-search.
     */
    private String field;
    /**
     * A custom name for the suggestions result (optional).
     */
    private String displayField;
    /**
     * Non-default options to use in the operation (optional).
     */
    private SuggestionOptions options;

    /**
     * {@inheritDoc}
     * @see SuggestionBase
     * @param field The index field in which to search for similar terms.
     */
    protected SuggestionBase(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDisplayField() {
        return displayField;
    }

    public void setDisplayField(String displayField) {
        this.displayField = displayField;
    }

    public SuggestionOptions getOptions() {
        return options;
    }

    public void setOptions(SuggestionOptions options) {
        this.options = options;
    }

}
