package net.ravendb.client.connection;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.closure.Function3;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.DatabaseDocument;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.utils.UrlUtils;


public class AdminRequestCreator {
  //url, method
  private final Function2<String, HttpMethods, HttpJsonRequest> createRequestForSystemDatabase;

  //currentServerUrl, operationUrl, method
  private final Function3<String, String, HttpMethods, HttpJsonRequest> createReplicationAwareRequest;

  public AdminRequestCreator(Function2<String, HttpMethods, HttpJsonRequest> createRequestForSystemDatabase,
    Function3<String, String, HttpMethods, HttpJsonRequest> createReplicationAwareRequest) {
    super();
    this.createRequestForSystemDatabase = createRequestForSystemDatabase;
    this.createReplicationAwareRequest = createReplicationAwareRequest;
  }

  public HttpJsonRequest createDatabase(DatabaseDocument databaseDocument, Reference<RavenJObject> docRef) {
    if (!databaseDocument.getSettings().containsKey("Raven/DataDir")) {
      throw new IllegalStateException("The Raven/DataDir setting is mandatory");
    }
    String dbname = databaseDocument.getId().replace("Raven/Databases/", "");
    MultiDatabase.assertValidName(dbname);
    RavenJObject doc = RavenJObject.fromObject(databaseDocument);
    doc.remove("id");
    docRef.value = doc;

    return createRequestForSystemDatabase.apply("/admin/databases/" + UrlUtils.escapeDataString(dbname), HttpMethods.PUT);
  }

  public HttpJsonRequest deleteDatabase(String databaseName, boolean hardDelete) {
    String deleteUrl = "/admin/databases/" + UrlUtils.escapeDataString(databaseName);

    if (hardDelete) {
      deleteUrl += "?hard-delete=true";
    }

    return createRequestForSystemDatabase.apply(deleteUrl, HttpMethods.DELETE);
  }

  public HttpJsonRequest stopIndexing(String serverUrl) {
    return createReplicationAwareRequest.apply(serverUrl, "/admin/StopIndexing", HttpMethods.POST);
  }

  public HttpJsonRequest startIndexing(String serverUrl, Integer maxNumberOfParallelIndexTasks) {
    String url = "/admin/StartIndexing";
    if (maxNumberOfParallelIndexTasks != null)
    {
        url += "?concurrency=" + maxNumberOfParallelIndexTasks;
    }
    return createReplicationAwareRequest.apply(serverUrl, url, HttpMethods.POST);
  }

  public HttpJsonRequest adminStats() {
    return createRequestForSystemDatabase.apply("/admin/stats", HttpMethods.GET);
  }

  @SuppressWarnings("unused")
  public HttpJsonRequest startBackup(String backupLocation, DatabaseDocument databaseDocument, String databaseName, boolean incremental) {
    if (databaseName == Constants.SYSTEM_DATABASE) {
        return createRequestForSystemDatabase.apply("/admin/backup", HttpMethods.POST);
    }
    return createRequestForSystemDatabase.apply("/databases/" + databaseName + "/admin/backup?incremental=" + incremental, HttpMethods.POST);
  }

  public HttpJsonRequest createRestoreRequest() {
      return createRequestForSystemDatabase.apply("/admin/restore", HttpMethods.POST);
  }

  public HttpJsonRequest indexingStatus(String serverUrl) {
    return createReplicationAwareRequest.apply(serverUrl, "/admin/IndexingStatus", HttpMethods.GET);
  }

  public HttpJsonRequest compactDatabase(String databaseName) {
    return createRequestForSystemDatabase.apply("/admin/compact?database=" + databaseName, HttpMethods.POST);
  }

  public HttpJsonRequest getDatabaseConfiguration(String serverUrl) {
    return createReplicationAwareRequest.apply(serverUrl, "/debug/config", HttpMethods.GET);
  }

  public String[] getDatabaseNames(int pageSize) {
    return getDatabaseNames(pageSize, 0);
  }

  /**
   * Gets the list of databases from the server
   * @param pageSize
   * @param start
   */
  @SuppressWarnings("boxing")
  public String[] getDatabaseNames(int pageSize, int start) {
    try (HttpJsonRequest requestForSystemDatabase = createRequestForSystemDatabase.apply(String.format("/databases?pageSize=%d&start=%d", pageSize, start), HttpMethods.GET)) {
      RavenJToken result = requestForSystemDatabase.readResponseJson();
      RavenJArray json = (RavenJArray) result;
      return json.values(String.class).toArray(new String[0]);
    }
  }

}
