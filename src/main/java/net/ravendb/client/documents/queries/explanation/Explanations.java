package net.ravendb.client.documents.queries.explanation;

import net.ravendb.client.documents.queries.QueryResult;

import java.util.Map;

public class Explanations {
    private Map<String, String[]> _explanations;

    public String[] getExplanations(String key) {
        String[] results = _explanations.get(key);
        return results;
    }

    public Map<String, String[]> getExplanations() {
        return _explanations;
    }

    public void setExplanations(Map<String, String[]> explanations) {
        _explanations = explanations;
    }

    public void update(QueryResult queryResult) {
        _explanations = queryResult.getExplanations();
    }
}
