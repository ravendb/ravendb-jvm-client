package net.ravendb.abstractions.data;

/**
 *  Represent a field sort options
 */
public class SortedField {

  private String field;

  private boolean descending;

  /**
   * Initializes a new instance of the {@link #SortedField(String) SortedField} class.
   * @param fieldWithPotentialPrefix The field with potential prefix.
   */
  public SortedField(String fieldWithPotentialPrefix) {
    if(fieldWithPotentialPrefix.startsWith("+")) {
      field = fieldWithPotentialPrefix.substring(1);
    } else if (fieldWithPotentialPrefix.startsWith("-")) {
      field = fieldWithPotentialPrefix.substring(1);
      descending = true;
    } else {
      field = fieldWithPotentialPrefix;
    }
  }

  @Override
  public SortedField clone() throws CloneNotSupportedException {
    return new SortedField((descending?"-":"")  + field);
  }

  /**
   * Index field name for sorting.
   */
  public String getField() {
    return field;
  }

  /**
   * Gets or sets a value indicating whether this {@link #SortedField(String) SortedField} is descending.
   * {@value true if descending; otherwise, false.}
   */
  public boolean isDescending() {
    return descending;
  }

  /**
   * Gets or sets a value indicating whether this {@link #SortedField(String) SortedField} is descending.
   * {@value true if descending; otherwise, false.}
   * @param descending
   */
  public void setDescending(boolean descending) {
    this.descending = descending;
  }

  /**
   * Index field name for sorting.
   * @param field
   */
  public void setField(String field) {
    this.field = field;
  }

}
