package net.ravendb.client.documents.queries.suggestions;

public class SuggestionOptions {

    public static final SuggestionOptions defaultOptions = new SuggestionOptions();

    public static final float DEFAULT_ACCURACY = 0.5f;

    public static final int DEFAULT_PAGE_SIZE = 15;

    public static final StringDistanceTypes DEFAULT_DISTANCE = StringDistanceTypes.LEVENSHTEIN;

    public static final SuggestionSortMode DEFAULT_SORT_MODE = SuggestionSortMode.POPULARITY;

    private int pageSize;

    private StringDistanceTypes distance;

    private Float accuracy;

    private SuggestionSortMode sortMode;

    public SuggestionOptions() {
        sortMode = DEFAULT_SORT_MODE;
        distance = DEFAULT_DISTANCE;
        accuracy = DEFAULT_ACCURACY;
        pageSize = DEFAULT_PAGE_SIZE;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return String distance algorithm to use. If null then default algorithm is used (Levenshtein).
     */
    public StringDistanceTypes getDistance() {
        return distance;
    }

    /**
     * @param distance String distance algorithm to use. If null then default algorithm is used (Levenshtein).
     */
    public void setDistance(StringDistanceTypes distance) {
        this.distance = distance;
    }

    /**
     * @return Suggestion accuracy. If null then default accuracy is used (0.5f).
     */
    public Float getAccuracy() {
        return accuracy;
    }

    /**
     * @param accuracy Suggestion accuracy. If null then default accuracy is used (0.5f).
     */
    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * @return Whether to return the terms in order of popularity
     */
    public SuggestionSortMode getSortMode() {
        return sortMode;
    }

    /**
     * @param sortMode Whether to return the terms in order of popularity
     */
    public void setSortMode(SuggestionSortMode sortMode) {
        this.sortMode = sortMode;
    }
}
