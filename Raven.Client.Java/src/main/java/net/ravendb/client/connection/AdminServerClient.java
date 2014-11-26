package net.ravendb.client.connection;

import java.io.IOException;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.closure.Function3;
import net.ravendb.abstractions.data.AdminStatistics;
import net.ravendb.abstractions.data.BuildNumber;
import net.ravendb.abstractions.data.DatabaseBackupRequest;
import net.ravendb.abstractions.data.DatabaseDocument;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.DatabaseRestoreRequest;
import net.ravendb.abstractions.exceptions.ServerClientException;
import net.ravendb.abstractions.indexing.IndexMergeResults;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.indexes.RavenDocumentsByEntityName;



public class AdminServerClient implements IAdminDatabaseCommands, IGlobalAdminDatabaseCommands {

  protected final ServerClient innerServerClient;
  protected final AdminRequestCreator adminRequest;

  public AdminServerClient(ServerClient serverClient) {
    innerServerClient = serverClient;
    adminRequest = new AdminRequestCreator(new Function2<String, HttpMethods, HttpJsonRequest>() {

      @Override
      public HttpJsonRequest apply(String url, HttpMethods method) {
        return ((ServerClient)innerServerClient.forSystemDatabase()).createRequest(method, url);
      }
    }, new Function2<String, HttpMethods, HttpJsonRequest>() {

      @Override
      public HttpJsonRequest apply(String url, HttpMethods method) {
        return innerServerClient.createRequest(method, url);
      }
    }, new Function3<String, String, HttpMethods, HttpJsonRequest>() {

      @Override
      public HttpJsonRequest apply(String currentServerUrl, String requestUrl, HttpMethods method) {
        return innerServerClient.createReplicationAwareRequest(currentServerUrl, requestUrl, method);
      }
    });
  }

  @Override
  public BuildNumber getBuildNumber() {
    return innerServerClient.getBuildNumber();
  }


  @Override
  public void createDatabase(DatabaseDocument databaseDocument) {
    Reference<RavenJObject> docRef = new Reference<>();
    HttpJsonRequest req = adminRequest.createDatabase(databaseDocument, docRef);

    req.write(docRef.value.toString());
    req.executeRequest();
  }

  @Override
  public void deleteDatabase(String databaseName)  {
    deleteDatabase(databaseName, false);

  }
  @Override
  public void deleteDatabase(String databaseName, boolean hardDelete) {
    adminRequest.deleteDatabase(databaseName, hardDelete).executeRequest();
  }

  @Override
  public IDatabaseCommands getCommands() {
    return innerServerClient;
  }

  @Override
  public Operation compactDatabase(String databaseName) {
    RavenJToken json = adminRequest.compactDatabase(databaseName).readResponseJson();
    return new Operation((ServerClient)innerServerClient.forSystemDatabase(), json.value(Long.class, "OperationId"));
  }

  @Override
  public void stopIndexing() {
    innerServerClient.executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Void>() {

      @Override
      public Void apply(OperationMetadata operationMetadata) {
        adminRequest.stopIndexing(operationMetadata.getUrl()).executeRequest();
        return null;
      }
    });
  }

  @Override
  public void startIndexing() {
    startIndexing(null);
  }

  @Override
  public void startIndexing(final Integer maxNumberOfParallelIndexTasks) {
    innerServerClient.executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Void>() {

      @Override
      public Void apply(OperationMetadata operationMetadata) {
        adminRequest.startIndexing(operationMetadata.getUrl(), maxNumberOfParallelIndexTasks).executeRequest();
        return null;
      }
    });
  }

  @Override
  public void startBackup(String backupLocation, DatabaseDocument databaseDocument, boolean incremental, String databaseName) {
    HttpJsonRequest request = adminRequest.startBackup(backupLocation, databaseDocument, databaseName, incremental);

    DatabaseBackupRequest backupRequest = new DatabaseBackupRequest();
    backupRequest.setBackupLocation(backupLocation);
    backupRequest.setDatabaseDocument(databaseDocument);

    request.write(RavenJObject.fromObject(backupRequest).toString());
    request.executeRequest();
  }

  @Override
  public Operation startRestore(DatabaseRestoreRequest restoreRequest) {
    HttpJsonRequest request = adminRequest.createRestoreRequest();
    request.write(RavenJObject.fromObject(restoreRequest).toString());
    RavenJToken jsonResponse = request.readResponseJson();
    return new Operation((ServerClient)innerServerClient.forSystemDatabase(), jsonResponse.value(Long.class, "OperationId"));
  }


  @Override
  public String getIndexingStatus() {
    return innerServerClient.executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, String>() {

      @Override
      public String apply(OperationMetadata operationMetadata) {
        RavenJToken result = adminRequest.indexingStatus(operationMetadata.getUrl()).readResponseJson();
        return result.value(String.class, "IndexingStatus");
      }
    });
  }

  @Override
  public RavenJObject getDatabaseConfiguration() {
    return innerServerClient.executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, RavenJObject>() {

      @Override
      public RavenJObject apply(OperationMetadata operationMetadata) {
        return (RavenJObject) adminRequest.getDatabaseConfiguration(operationMetadata.getUrl()).readResponseJson();
      }
    });
  }

  @Override
  public String[] getDatabaseNames(int pagesize) {
    return getDatabaseNames(pagesize, 0);
  }

  @Override
  public String[] getDatabaseNames(int pageSize, int start) {
    return adminRequest.getDatabaseNames(pageSize, start);
  }

  @Override
  public AdminStatistics getStatistics() {
    try {
      RavenJObject json = (RavenJObject)adminRequest.adminStats().readResponseJson();
      return innerServerClient.convention.createSerializer().readValue(json.toString(), AdminStatistics.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void ensureDatabaseExists(String name, boolean ignoreFailures) {

    ServerClient serverClient = (ServerClient) innerServerClient.forSystemDatabase();

    try (AutoCloseable readFromMaster = serverClient.forceReadFromMaster()) {
      DatabaseDocument doc = MultiDatabase.createDatabaseDocument(name);

      try {
        if (serverClient.get(doc.getId()) != null) {
          return;
        }
        serverClient.getGlobalAdmin().createDatabase(doc);
      } catch (Exception e) {
        if (!ignoreFailures) {
          throw new RuntimeException(e);
        }
      }

      try {
        new RavenDocumentsByEntityName().execute(serverClient.forDatabase(name), new DocumentConvention());
      } catch (Exception e) {
        // we really don't care if this fails, and it might, if the user doesn't have permissions on the new db
      }

    } catch (Exception e) {
        // we really don't care if this fails, and it might, if the user doesn't have permissions on the new db
    }
  }

  @Override
  public void ensureDatabaseExists(String name) {
    ensureDatabaseExists(name, false);
  }


}
