package net.ravendb.abstractions.data;

public class SuggestionQuery {

  public static float DEFAULT_ACCURACY = 0.5f;

  public static int DEFAULT_MAX_SUGGESTIONS = 15;

  public static StringDistanceTypes DEFAULT_DISTANCE = StringDistanceTypes.LEVENSHTEIN;


  /**
   * Create a new instance of {@link SuggestionQuery}
   */
  public SuggestionQuery() {
    maxSuggestions = DEFAULT_MAX_SUGGESTIONS;
    distance = StringDistanceTypes.LEVENSHTEIN;
  }


  public SuggestionQuery(String field, String term) {
    this();
    this.term = term;
    this.field = field;
  }



  private String term;
  private String field;
  private int maxSuggestions;
  private StringDistanceTypes distance;
  private Float accuracy;
  private boolean popularity;

  /**
   * Term is what the user likely entered, and will used as the basis of the suggestions.
   */
  public String getTerm() {
    return term;
  }

  /**
   * Term is what the user likely entered, and will used as the basis of the suggestions.
   * @param term
   */
  public void setTerm(String term) {
    this.term = term;
  }

  /**
   * Field to be used in conjunction with the index.
   */
  public String getField() {
    return field;
  }

  /**
   * Field to be used in conjunction with the index.
   * @param field
   */
  public void setField(String field) {
    this.field = field;
  }

  /**
   * Maximum number of suggestions to return.
   * Value:
   * Default value is 15.
   * {@value Default value is 15.}
   */
  public int getMaxSuggestions() {
    return maxSuggestions;
  }

  /**
   * * Maximum number of suggestions to return.
   * Value:
   * Default value is 15.
   * {@value Default value is 15.}
   * @param maxSuggestions
   */
  public void setMaxSuggestions(int maxSuggestions) {
    this.maxSuggestions = maxSuggestions;
  }

  /**
   * String distance algorithm to use. If null then default algorithm is used (Levenshtein).
   */
  public StringDistanceTypes getDistance() {
    return distance;
  }

  /**
   * String distance algorithm to use. If null then default algorithm is used (Levenshtein).
   * @param distance
   */
  public void setDistance(StringDistanceTypes distance) {
    this.distance = distance;
  }

  /**
   * Suggestion accuracy. If null then default accuracy is used (0.5f).
   */
  public Float getAccuracy() {
    return accuracy;
  }

  /**
   * Suggestion accuracy. If null then default accuracy is used (0.5f).
   * @param accuracy
   */
  public void setAccuracy(Float accuracy) {
    this.accuracy = accuracy;
  }

  /**
   * Whatever to return the terms in order of popularity
   */
  public boolean isPopularity() {
    return popularity;
  }

  /**
   * Whatever to return the terms in order of popularity
   * @param popularity
   */
  public void setPopularity(boolean popularity) {
    this.popularity = popularity;
  }





}
