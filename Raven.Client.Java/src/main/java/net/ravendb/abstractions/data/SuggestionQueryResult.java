package net.ravendb.abstractions.data;

/**
 * The result of the suggestion query
 */
public class SuggestionQueryResult {

  private String[] suggestions;

  /**
   * Suggestions based on the term and dictionary.
   */
  public String[] getSuggestions() {
    return suggestions;
  }

  /**
   * Suggestions based on the term and dictionary.
   * @param suggestions
   */
  public void setSuggestions(String[] suggestions) {
    this.suggestions = suggestions;
  }


}
