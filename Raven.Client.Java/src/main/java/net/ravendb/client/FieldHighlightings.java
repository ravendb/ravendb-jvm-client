package net.ravendb.client;

import java.util.Map;
import java.util.TreeMap;

import net.ravendb.abstractions.data.QueryResult;


/**
 *  Query highlightings for the documents.
 */
public class FieldHighlightings {
  private final Map<String, String[]> highlightings;
  private String fieldName;

  public FieldHighlightings(String fieldName) {
    this.fieldName = fieldName;
    this.highlightings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * The field name.
   */
  public String getFieldName() {
    return fieldName;
  }


  public Map<String, String[]> getHighlightings() {
    return highlightings;
  }

  /**
   * Returns the list of document's field highlighting fragments.
   * @param documentId The document id, or the map/reduce key field.
   */
  public String[] getFragments(String documentId) {
    if (this.highlightings.containsKey(documentId)) {
      return highlightings.get(documentId);
    } else {
      return new String[0];
    }
  }

  protected void update(QueryResult queryResult) {
    this.highlightings.clear();

    if (queryResult.getHighlightings() == null) {
      return;
    }

    for (Map.Entry<String, Map<String, String[]>> entityFragments : queryResult.getHighlightings().entrySet()) {
      for(Map.Entry<String, String[]> fieldFragments : entityFragments.getValue().entrySet()) {
        if (fieldFragments.getKey().equals(this.fieldName)) {
          this.highlightings.put(entityFragments.getKey(), fieldFragments.getValue());
        }
      }
    }
  }

}
