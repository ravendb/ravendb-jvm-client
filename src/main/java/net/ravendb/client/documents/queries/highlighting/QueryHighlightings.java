package net.ravendb.client.documents.queries.highlighting;

import net.ravendb.client.documents.queries.QueryResult;

import java.util.ArrayList;
import java.util.List;

public class QueryHighlightings {
    private final List<Highlightings> _highlightings = new ArrayList<>();

    public Highlightings add(String fieldName) {
        Highlightings fieldHighlightings = new Highlightings(fieldName);
        _highlightings.add(fieldHighlightings);
        return fieldHighlightings;
    }

    public void update(QueryResult queryResult) {
        for (Highlightings fieldHighlightings : _highlightings) {
            fieldHighlightings.update(queryResult.getHighlightings());
        }
    }

}
