package net.ravendb.abstractions.exceptions;

public class IndexCompilationException extends RuntimeException {

  private String indexDefinitionProperty;
  private String problematicText;

  public IndexCompilationException() {
    super();
  }

  public IndexCompilationException(String message, Throwable cause) {
    super(message, cause);
  }

  public IndexCompilationException(String message) {
    super(message);
  }

  public IndexCompilationException(Throwable cause) {
    super(cause);
  }

  /**
   * Indicates which property caused error (Maps, Reduce).
   */
  public String getIndexDefinitionProperty() {
    return indexDefinitionProperty;
  }

  /**
   * Indicates which property caused error (Maps, Reduce).
   * @param indexDefinitionProperty
   */
  public void setIndexDefinitionProperty(String indexDefinitionProperty) {
    this.indexDefinitionProperty = indexDefinitionProperty;
  }

  /**
   * Value of a problematic property.
   */
  public String getProblematicText() {
    return problematicText;
  }

  /**
   * Value of a problematic property.
   * @param problematicText
   */
  public void setProblematicText(String problematicText) {
    this.problematicText = problematicText;
  }



}
