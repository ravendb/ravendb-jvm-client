package net.ravendb.client.documents.queries.suggestions;

public class SuggestionBase {

    private String field;
    private String displayField;
    private SuggestionOptions options;

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
