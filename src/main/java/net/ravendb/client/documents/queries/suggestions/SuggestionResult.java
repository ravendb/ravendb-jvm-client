package net.ravendb.client.documents.queries.suggestions;

import java.util.ArrayList;
import java.util.List;

public class SuggestionResult {

    private String name;

    private List<String> suggestions;

    public SuggestionResult() {
        suggestions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

}
