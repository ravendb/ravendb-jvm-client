package net.ravendb.abstractions.data;

import org.apache.commons.lang.StringUtils;

public class FacetValue {
  private String range;
  private int hits;
  private Integer count;
  private Double sum;
  private Double max;
  private Double min;
  private Double average;

  public Double getAggregation(FacetAggregation aggregation) {
    switch(aggregation) {
    case NONE:
      return null;
    case COUNT:
      return count.doubleValue();
    case MAX:
      return max;
    case MIN:
      return min;
    case AVERAGE:
      return average;
    case SUM:
      return sum;
    default:
      return null;
    }
  }

  @Override
  public String toString() {
    String msg = range + " -  Hits: " + hits + ",";
    if (count != null) {
      msg += "Count: " + count + ",";
    }
    if(sum != null) {
      msg += "Sum: " + sum + ",";
    }
    if (max != null) {
      msg += "Max: " + max + ",";
    }
    if (min != null) {
      msg += "Min: " + min + ",";
    }
    if (average != null) {
      msg += "Average: " + average + ",";
    }

    msg = StringUtils.stripEnd(msg, " ,");
    return msg;
  }

  /**
   * Stores average value if FacetAggregation.Average was set.
   */
  public Double getAverage() {
    return average;
  }

  /**
   * Stores count value if FacetAggregation.Count was set.
   */
  public Integer getCount() {
    return count;
  }

  /**
   * Number of terms that are covered by this facet.
   */
  public int getHits() {
    return hits;
  }

  /**
   * Stores maximum value if FacetAggregation.Max was set.
   */
  public Double getMax() {
    return max;
  }

  /**
   * Stores minimum value if FacetAggregation.Min was set.
   */
  public Double getMin() {
    return min;
  }

  /**
   * Name of range for which facet value applies.
   */
  public String getRange() {
    return range;
  }

  /**
   * Stores sum of all values if FacetAggregation.Sum was set.
   */
  public Double getSum() {
    return sum;
  }

  /**
   * Stores average value if FacetAggregation.Average was set.
   * @param average
   */
  public void setAverage(Double average) {
    this.average = average;
  }

  /**
   * Stores count value if FacetAggregation.Count was set.
   * @param count
   */
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   * Number of terms that are covered by this facet.
   * @param hits
   */
  public void setHits(int hits) {
    this.hits = hits;
  }

  /**
   * Stores maximum value if FacetAggregation.Max was set.
   * @param max
   */
  public void setMax(Double max) {
    this.max = max;
  }

  /**
   * Stores minimum value if FacetAggregation.Min was set.
   * @param min
   */
  public void setMin(Double min) {
    this.min = min;
  }

  /**
   * Name of range for which facet value applies.
   * @param range
   */
  public void setRange(String range) {
    this.range = range;
  }

  /**
   * Stores sum of all values if FacetAggregation.Sum was set.
   * @param sum
   */
  public void setSum(Double sum) {
    this.sum = sum;
  }


}
