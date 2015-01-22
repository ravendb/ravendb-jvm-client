package net.ravendb.abstractions.data;

import java.util.ArrayList;
import java.util.List;

public class FacetResult {
  private List<FacetValue> values;
  private List<String> remainingTerms;
  private int remainingTermsCount;
  private int remainingHits;

  public FacetResult() {
    values = new ArrayList<>();
    remainingTerms = new ArrayList<>();
  }

  /**
   * The number of remaining hits outside of those covered by the Values terms.
   */
  public int getRemainingHits() {
    return remainingHits;
  }

  /**
   * A list of remaining terms in term sort order for terms that are outside of the MaxResults count.
   */
  public List<String> getRemainingTerms() {
    return remainingTerms;
  }

  /**
   * The number of remaining terms outside of those covered by the Values terms.
   */
  public int getRemainingTermsCount() {
    return remainingTermsCount;
  }

  /**
   * The facet terms and hits up to a limit of MaxResults items (as specified in the facet setup document), sorted
   * in TermSortMode order (as indicated in the facet setup document).
   */
  public List<FacetValue> getValues() {
    return values;
  }

  /**
   * The number of remaining hits outside of those covered by the Values terms.
   * @param remainingHits
   */
  public void setRemainingHits(int remainingHits) {
    this.remainingHits = remainingHits;
  }

  /**
   * A list of remaining terms in term sort order for terms that are outside of the MaxResults count.
   * @param remainingTerms
   */
  public void setRemainingTerms(List<String> remainingTerms) {
    this.remainingTerms = remainingTerms;
  }

  /**
   * The number of remaining terms outside of those covered by the Values terms.
   * @param remainingTermsCount
   */
  public void setRemainingTermsCount(int remainingTermsCount) {
    this.remainingTermsCount = remainingTermsCount;
  }

  /**
   * The facet terms and hits up to a limit of MaxResults items (as specified in the facet setup document), sorted
   * in TermSortMode order (as indicated in the facet setup document).
   * @param values
   */
  public void setValues(List<FacetValue> values) {
    this.values = values;
  }


}
