package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FacetOptions {

    static final FacetOptions defaultOptions = new FacetOptions();

    @JsonProperty("TermSortMode")
    private FacetTermSortMode termSortMode;

    @JsonProperty("IncludeRemainingTerms")
    private boolean includeRemainingTerms;

    @JsonProperty("Start")
    private int start;

    @JsonProperty("PageSize")
    private int pageSize;

    public FacetOptions() {
        pageSize = Integer.MAX_VALUE;
    }

    public static FacetOptions getDefaultOptions() {
        return defaultOptions;
    }

    public FacetTermSortMode getTermSortMode() {
        return termSortMode;
    }

    public void setTermSortMode(FacetTermSortMode termSortMode) {
        this.termSortMode = termSortMode;
    }

    /**
     * @return Indicates if remaining terms should be included in results.
     */
    public boolean isIncludeRemainingTerms() {
        return includeRemainingTerms;
    }

    /**
     * @param includeRemainingTerms Indicates if remaining terms should be included in results.
     */
    public void setIncludeRemainingTerms(boolean includeRemainingTerms) {
        this.includeRemainingTerms = includeRemainingTerms;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
