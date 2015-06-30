package net.ravendb.client.indexes;

import net.ravendb.abstractions.data.ScriptedIndexResults;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;


public abstract class AbstractScriptedIndexCreationTask {
  private final ScriptedIndexResults scripts;

  protected AbstractScriptedIndexCreationTask(String indexName) {
    scripts = new ScriptedIndexResults();
    scripts.setId(ScriptedIndexResults.ID_PREFIX + indexName);
  }

  protected <T extends AbstractIndexCreationTask> AbstractScriptedIndexCreationTask(T index) {
    this(index.getClass().getName().replaceAll("_", "/"));
  }

  protected String getIndexScript() {
    return scripts.getIndexScript();
  }

  protected void setIndexScript(String value) {
    scripts.setIndexScript(value);
  }

  protected String getDeleteScript() {
    return scripts.getDeleteScript();
  }

  protected void setDeleteScript(String value) {
    scripts.setDeleteScript(value);
  }

  public boolean isRetryOnconcurrencyExceptions() {
    return scripts.isRetryOnconcurrencyExceptions();
  }

  public void setRetryOnconcurrencyExceptions(boolean retryOnconcurrencyExceptions) {
    scripts.setRetryOnconcurrencyExceptions(retryOnconcurrencyExceptions);
  }

  public void execute(IDocumentStore store) {
    store.getDatabaseCommands().put(scripts.getId(), null, RavenJObject.fromObject(scripts), null);
  }

  public void execute(IDatabaseCommands databaseCommands) {
    databaseCommands.put(scripts.getId(), null, RavenJObject.fromObject(scripts), null);
  }
}
