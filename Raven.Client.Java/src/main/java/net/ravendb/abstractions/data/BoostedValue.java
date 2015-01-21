package net.ravendb.abstractions.data;

public class BoostedValue {
  private float boost;
  private Object value;

  /**
   * Boost factor.
   */
  public float getBoost() {
    return boost;
  }

  /**
   * Boost factor.
   * @param boost
   */
  public void setBoost(float boost) {
    this.boost = boost;
  }

  /**
   *
   */
  public Object getValue() {
    return value;
  }

  /**
   * Boosted value.
   * @param value
   */
  public void setValue(Object value) {
    this.value = value;
  }


}
