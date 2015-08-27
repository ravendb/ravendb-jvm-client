package net.ravendb.client.indexes;

import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.ScriptedIndexResults;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentConvention;


public abstract class AbstractScriptedIndexCreationTask extends AbstractIndexCreationTask {
  private final ScriptedIndexResults scripts;

  protected AbstractScriptedIndexCreationTask() {
    scripts = new ScriptedIndexResults();
  }

  public String getIndexScript() {
    return scripts.getIndexScript();
  }

  public void setIndexScript(String value) {
    scripts.setIndexScript(value);
  }

  public String getDeleteScript() {
    return scripts.getDeleteScript();
  }

  public void setDeleteScript(String value) {
    scripts.setDeleteScript(value);
  }

  public boolean isRetryOnconcurrencyExceptions() {
    return scripts.isRetryOnconcurrencyExceptions();
  }

  public void setRetryOnconcurrencyExceptions(boolean retryOnconcurrencyExceptions) {
    scripts.setRetryOnconcurrencyExceptions(retryOnconcurrencyExceptions);
  }

  @Override
  public void afterExecute(IDatabaseCommands databaseCommands, DocumentConvention documentConvention) {
    super.afterExecute(databaseCommands, documentConvention);
    afterExecute(databaseCommands, getIndexName(), scripts);
  }

  protected static void afterExecute(IDatabaseCommands databaseCommands, String indexName, ScriptedIndexResults scripts) {
    String documentId = getScriptedIndexResultsDocumentId(indexName);
    scripts.setId(documentId);

    JsonDocument oldDocument = databaseCommands.get(documentId);
    RavenJObject newDocument = RavenJObject.fromObject(scripts);

    if (oldDocument != null && RavenJToken.deepEquals(oldDocument.getDataAsJson(), newDocument)) {
      return;
    }

    databaseCommands.put(documentId, null, newDocument, null);
    databaseCommands.resetIndex(indexName);
  }

  public void execute(IDocumentStore store) {
    store.getDatabaseCommands().put(scripts.getId(), null, RavenJObject.fromObject(scripts), null);
  }

  public void execute(IDatabaseCommands databaseCommands) {
    databaseCommands.put(scripts.getId(), null, RavenJObject.fromObject(scripts), null);
  }

  private static String getScriptedIndexResultsDocumentId(String indexName) {
    return ScriptedIndexResults.ID_PREFIX + indexName;
  }
}
