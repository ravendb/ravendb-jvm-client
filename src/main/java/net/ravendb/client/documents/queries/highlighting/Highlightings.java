package net.ravendb.client.documents.queries.highlighting;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *  Query highlightings for the documents.
 */
public class Highlightings {

    private final Map<String, String[]> _highlightings;
    private String _fieldName;

    public Highlightings(String fieldName) {
        _fieldName = fieldName;
        _highlightings = new TreeMap<>(String::compareToIgnoreCase);
    }

    public String getFieldName() {
        return _fieldName;
    }

    public Set<String> getResultIndents() {
        return _highlightings.keySet();
    }

    /**
     * @param key  The document id, or the map/reduce key field.
     * @return Returns the list of document's field highlighting fragments.
     */
    public String[] getFragments(String key) {
        String[] result = _highlightings.get(key);
        if (result == null) {
            return new String[0];
        }
        return result;
    }

    public void update(Map<String, Map<String, String[]>> highlightings) {
        _highlightings.clear();

        if (highlightings == null || !highlightings.containsKey(getFieldName())) {
            return;
        }

        Map<String, String[]> result = highlightings.get(getFieldName());
        for (Map.Entry<String, String[]> kvp : result.entrySet()) {
            _highlightings.put(kvp.getKey(), kvp.getValue());
        }
    }
}
