package net.ravendb.abstractions.data;


public class ScriptedIndexResults {

  public final static String ID_PREFIX = "Raven/ScriptedIndexResults/";

  private String id;
  private String indexScript;
  private String deleteScript;
  private boolean retryOnconcurrencyExceptions;

  /**
   * Identifier for ScriptedIndexResults document.
   */
  public String getId() {
    return id;
  }

  /**
   * Identifier for ScriptedIndexResults document.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Script that will be evaluated for each indexed document.
   */
  public String getIndexScript() {
    return indexScript;
  }

  /**
   * Script that will be evaluated for each indexed document.
   */
  public void setIndexScript(String indexScript) {
    this.indexScript = indexScript;
  }

  /**
   * Script that will be evaluated for each document deleted from the index.
   */
  public String getDeleteScript() {
    return deleteScript;
  }

  /**
   * Script that will be evaluated for each document deleted from the index.
   */
  public void setDeleteScript(String deleteScript) {
    this.deleteScript = deleteScript;
  }

  /**
   *  Indicates if patcher should retry applying the scripts when concurrency exception occurs.
   *  If <code>false</code> then exception will be thrown and indexing will fail for this particular batch.
   */
  public boolean isRetryOnconcurrencyExceptions() {
    return retryOnconcurrencyExceptions;
  }

  /**
   *  Indicates if patcher should retry applying the scripts when concurrency exception occurs.
   *  If <code>false</code> then exception will be thrown and indexing will fail for this particular batch.
   */
  public void setRetryOnconcurrencyExceptions(boolean retryOnconcurrencyExceptions) {
    this.retryOnconcurrencyExceptions = retryOnconcurrencyExceptions;
  }

  public ScriptedIndexResults() {
    super();
    this.retryOnconcurrencyExceptions = true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((deleteScript == null) ? 0 : deleteScript.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((indexScript == null) ? 0 : indexScript.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ScriptedIndexResults other = (ScriptedIndexResults) obj;
    if (deleteScript == null) {
      if (other.deleteScript != null) return false;
    } else if (!deleteScript.equals(other.deleteScript)) return false;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (indexScript == null) {
      if (other.indexScript != null) return false;
    } else if (!indexScript.equals(other.indexScript)) return false;
    return true;
  }
}
