package net.ravendb.client;

/**
 * Parameters for the Where Equals call
 */
public class WhereParams {

  private String fieldName;
  private Object value;
  private boolean isAnalyzed;
  private Class<?> fieldTypeForIdentifier;
  private boolean allowWildcards;
  private boolean isNestedPath;

  public String getFieldName() {
    return fieldName;
  }
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
  /**
   * @return Should the field be analyzed
   */
  public boolean isAnalyzed() {
    return isAnalyzed;
  }

  /**
   * Should the field be analyzed
   * @param isAnalyzed
   */
  public void setAnalyzed(boolean isAnalyzed) {
    this.isAnalyzed = isAnalyzed;
  }

  public Class< ? > getFieldTypeForIdentifier() {
    return fieldTypeForIdentifier;
  }

  public void setFieldTypeForIdentifier(Class< ? > fieldTypeForIdentifier) {
    this.fieldTypeForIdentifier = fieldTypeForIdentifier;
  }

  /**
   * @return Should the field allow wildcards
   */
  public boolean isAllowWildcards() {
    return allowWildcards;
  }

  /**
   * Should the field allow wildcards
   * @param allowWildcards
   */
  public void setAllowWildcards(boolean allowWildcards) {
    this.allowWildcards = allowWildcards;
  }

  /**
   * @return Is this a root property or not?
   */
  public boolean isNestedPath() {
    return isNestedPath;
  }

  /**
   * Is this a root property or not?
   * @param isNestedPath
   */
  public void setNestedPath(boolean isNestedPath) {
    this.isNestedPath = isNestedPath;
  }

  /**
   *  Create a new instance
   */
  public WhereParams() {
    isNestedPath = false;
    allowWildcards = false;
    isAnalyzed = true;
  }
}
