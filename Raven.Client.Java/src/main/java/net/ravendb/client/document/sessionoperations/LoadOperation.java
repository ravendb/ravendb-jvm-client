package net.ravendb.client.document.sessionoperations;

import java.util.Date;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;

import com.google.common.base.Defaults;


public class LoadOperation {

  private static final ILog log = LogManager.getCurrentClassLogger();
  private final InMemoryDocumentSessionOperations sessionOperations;
  private final Function0<CleanCloseable> disableAllCaching;
  private final String id;
  private boolean firstRequest = true;
  private JsonDocument documentFound;

  private long spStart;


  public LoadOperation(InMemoryDocumentSessionOperations sessionOperations, Function0<CleanCloseable> disableAllCaching, String id) {
    if (id == null) {
      throw new IllegalArgumentException("The document is cannot be null");
    }
    this.sessionOperations = sessionOperations;
    this.disableAllCaching = disableAllCaching;
    this.id = id;
  }

  public void logOperation() {
    log.debug("Loading document [%s] from %s", id, sessionOperations.getStoreIdentifier());
  }

  public CleanCloseable enterLoadContext() {
    if (firstRequest == false) { // if this is a repeated request, we mustn't use the cached result, but have to re-query the server
      return disableAllCaching.apply();
    }
    spStart = new Date().getTime();
    return null;
  }

  @SuppressWarnings("boxing")
  public boolean setResult(JsonDocument document) {
    firstRequest = false;
    documentFound = document;
    if (documentFound == null) {
      return false;
    }
    return
        Boolean.TRUE.equals(documentFound.getNonAuthoritativeInformation())
        && sessionOperations.isAllowNonAuthoritativeInformation() == false
        && (new Date().getTime() - spStart) < sessionOperations.getNonAuthoritativeInformationTimeout();
  }

  @SuppressWarnings("unchecked")
  public <T> T complete(Class<T> clazz) {
    if (documentFound == null) {
      sessionOperations.registerMissing(id);
      return Defaults.defaultValue(clazz);
    }
    return (T) sessionOperations.trackEntity(clazz, documentFound);
  }

}
