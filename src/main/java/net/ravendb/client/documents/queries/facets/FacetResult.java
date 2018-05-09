package net.ravendb.client.documents.queries.facets;

import java.util.ArrayList;
import java.util.List;

public class FacetResult {

    private String name;

    private List<FacetValue> values;

    private List<String> remainingTerms;

    private int remainingTermsCount;

    private int remainingHits;

    public FacetResult() {
        values = new ArrayList<>();
        remainingTerms = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The facet terms and hits up to a limit of MaxResults items (as specified in the facet setup document), sorted
     * in TermSortMode order (as indicated in the facet setup document).
     * @return values
     */
    public List<FacetValue> getValues() {
        return values;
    }

    /**
     * The facet terms and hits up to a limit of MaxResults items (as specified in the facet setup document), sorted
     * in TermSortMode order (as indicated in the facet setup document).
     * @param values values to set
     */
    public void setValues(List<FacetValue> values) {
        this.values = values;
    }

    /**
     * @return A list of remaining terms in term sort order for terms that are outside of the MaxResults count.
     */
    public List<String> getRemainingTerms() {
        return remainingTerms;
    }

    /**
     * @param remainingTerms A list of remaining terms in term sort order for terms that are outside of the MaxResults count.
     */
    public void setRemainingTerms(List<String> remainingTerms) {
        this.remainingTerms = remainingTerms;
    }

    /**
     * @return The number of remaining terms outside of those covered by the Values terms.
     */
    public int getRemainingTermsCount() {
        return remainingTermsCount;
    }

    /**
     * @param remainingTermsCount The number of remaining terms outside of those covered by the Values terms.
     */
    public void setRemainingTermsCount(int remainingTermsCount) {
        this.remainingTermsCount = remainingTermsCount;
    }

    /**
     * @return The number of remaining hits outside of those covered by the Values terms.
     */
    public int getRemainingHits() {
        return remainingHits;
    }

    /**
     * @param remainingHits The number of remaining hits outside of those covered by the Values terms.
     */
    public void setRemainingHits(int remainingHits) {
        this.remainingHits = remainingHits;
    }

}
