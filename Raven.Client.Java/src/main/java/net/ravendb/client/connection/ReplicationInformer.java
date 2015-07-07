package net.ravendb.client.connection;

import net.ravendb.abstractions.basic.EventArgs;
import net.ravendb.abstractions.cluster.ClusterInformation;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDestinationWithClusterInformation;
import net.ravendb.abstractions.replication.ReplicationDocumentWithClusterInformation;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.connection.request.FailureCounters;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.extensions.MultiDatabase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class ReplicationInformer extends ReplicationInformerBase<ServerClient> implements IDocumentStoreReplicationInformer {

  private final Object replicationLock = new Object();

  private boolean firstTime = true;

  private Date lastReplicationUpdate = new Date(0);

  private ReplicationDestination[] failoverServers;

  protected Thread refreshReplicationInformationTask;

  @Override
  public void setFailoverServers(ReplicationDestination[] failoverServers) {
    this.failoverServers = failoverServers;
  }

  @Override
  public void clearReplicationInformationLocalCache(ServerClient client) {
    String serverHash = ServerHash.getServerHash(client.getUrl());
    ReplicationInformerLocalCache.clearReplicationInformationFromLocalCache(serverHash);
  }

  @Override
  public ReplicationDestination[] getFailoverServers() {
    return failoverServers;
  }

  public ReplicationInformer(DocumentConvention conventions, HttpJsonRequestFactory jsonRequestFactory) {
    super(conventions, jsonRequestFactory, 1000);
  }

  @Override
  public void updateReplicationInformationIfNeeded(final ServerClient serverClient) {
    if (conventions.getFailoverBehavior().contains(FailoverBehavior.FAIL_IMMEDIATELY)) {
      return;//new CompletedFuture<>();
    }

    if (DateUtils.addMinutes(lastReplicationUpdate, 5).after(new Date())) {
      return;//new CompletedFuture<>();
    }

    synchronized (replicationLock) {
      if (firstTime) {
        String serverHash = ServerHash.getServerHash(serverClient.getUrl());

        JsonDocument document = ReplicationInformerLocalCache.tryLoadReplicationInformationFromLocalCache(serverHash);
        if (!isInvalidDestinationsDocument(document)) {
          updateReplicationInformationFromDocument(document);
        }
      }

      firstTime = false;

      if (DateUtils.addMinutes(lastReplicationUpdate, 5).after(new Date())) {
        return; //new CompletedFuture<>();
      }

      Thread taskCopy = refreshReplicationInformationTask;

      if (taskCopy != null) {
        return; //taskCopy;
      }

      refreshReplicationInformationTask = new Thread( new Runnable() {

        @Override
        public void run() {
          try {
            refreshReplicationInformation(serverClient);
            refreshReplicationInformationTask = null;
          } catch (Exception e) {
            log.error("Failed to refresh replication information", e);
          }
        }
      } );

      refreshReplicationInformationTask.start();
    }
  }

  @Override
  protected String getServerCheckUrl(String baseUrl) {
      return baseUrl + "/replication/topology?check-server-reachable";
  }

  @SuppressWarnings("hiding")
  @Override
  public void refreshReplicationInformation(ServerClient commands) {
    synchronized (this) {
      String serverHash = ServerHash.getServerHash(commands.getUrl());

      JsonDocument document;
      try {
        RavenJObject replicationDestinations = RavenJObject.fromObject(commands.directGetReplicationDestinations(new OperationMetadata(commands.getUrl(), commands.getPrimaryCredentials(), ClusterInformation.NOT_IN_CLUSTER)));
        document =  replicationDestinations == null ? null : SerializationHelper.toJsonDocument(replicationDestinations);
        getFailureCounters().getFailureCounts().put(commands.getUrl(), new FailureCounters.FailureCounter()); // we just hit the master, so we can reset its failure count
      } catch (Exception e) {
        log.error("Could not contact master for new replication information", e);
        document = ReplicationInformerLocalCache.tryLoadReplicationInformationFromLocalCache(serverHash);
      }
      if (document == null) {
        lastReplicationUpdate = new Date(); // checked and not found
        return;
      }

      ReplicationInformerLocalCache.trySavingReplicationInformationToLocalCache(serverHash, document);
      updateReplicationInformationFromDocument(document);
      lastReplicationUpdate = new Date();
    }
  }

  public void updateReplicationInformationFromDocument(JsonDocument document) {
    ReplicationDocumentWithClusterInformation replicationDocument = null;
    try {
      replicationDocument = JsonExtensions.createDefaultJsonSerializer().readValue(document.getDataAsJson().toString(),
              ReplicationDocumentWithClusterInformation.class);
    } catch (IOException e) {
      log.error("Mapping Exception", e);
      return;
    }
    replicationDestinations = new ArrayList<>();
    for (ReplicationDestinationWithClusterInformation x : replicationDocument.getDestinations()) {
      String url = StringUtils.isEmpty(x.getClientVisibleUrl()) ? x.getUrl() : x.getClientVisibleUrl();
      if (StringUtils.isEmpty(url)) {
        return;
      }
      if (!x.canBeFailover()) {
        return;
      }
      if (StringUtils.isEmpty(x.getDatabase())) {
        replicationDestinations.add(new OperationMetadata(url, new OperationCredentials(x.getApiKey()), x.getClusterInformation()));
        return;
      }
      replicationDestinations.add(new OperationMetadata(MultiDatabase.getRootDatabaseUrl(url) + "/databases/"
        + x.getDatabase(), new OperationCredentials(x.getApiKey()), x.getClusterInformation()));
    }

    for (OperationMetadata replicationDestination : replicationDestinations) {
      if (!getFailureCounters().getFailureCounts().containsKey(replicationDestination.getUrl())) {
        getFailureCounters().getFailureCounts().put(replicationDestination.getUrl(), new FailureCounters.FailureCounter());
      }
    }

    if (replicationDocument.getClientConfiguration() != null) {
      conventions.updateFrom(replicationDocument.getClientConfiguration());
    }
  }


  public static class FailoverStatusChangedEventArgs extends EventArgs {

    public FailoverStatusChangedEventArgs() {
    }

    public FailoverStatusChangedEventArgs(String url, Boolean failing) {
      super();
      this.failing = failing;
      this.url = url;
    }

    private Boolean failing;
    private String url;

    /**
     * @return the failing
     */
    public Boolean getFailing() {
      return failing;
    }

    /**
     * @param failing the failing to set
     */
    public void setFailing(Boolean failing) {
      this.failing = failing;
    }

    /**
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
      this.url = url;
    }

  }


  @Override
  public void close() {
    Thread informationTask = refreshReplicationInformationTask;
    if (informationTask != null) {
      try {
        informationTask.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
