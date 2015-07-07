package net.ravendb.client.document;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.DatabaseStatistics;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.replication.ReplicatedEtagInfo;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDocumentWithClusterInformation;
import net.ravendb.client.connection.*;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.utils.CancellationTokenSource;
import net.ravendb.client.utils.CancellationTokenSource.CancellationToken;
import net.ravendb.client.utils.TimeSpan;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;


public class ReplicationBehavior implements CleanCloseable {

  public final static class DestinationAndSourceCollections {
    private String destination;
    private String[] sourceCollections;

    public DestinationAndSourceCollections(String destination, String[] sourceCollections) {
      this.destination = destination;
      this.sourceCollections = sourceCollections;
    }

    public String getDestination() {
      return destination;
    }

    public String[] getSourceCollections() {
      return sourceCollections;
    }
  }

  private final DocumentStore documentStore;
  private final static ILog log = LogManager.getCurrentClassLogger();


  private ExecutorService executor;

  public ReplicationBehavior(DocumentStore documentStore) {
    super();
    this.documentStore = documentStore;
  }

  public int waitSync() throws TimeoutException {
    return waitSync(null, null, null, 2);
  }

  private synchronized List<Future<Void>> whenAll(Collection<Callable<Void>> tasks) {
    if (this.executor == null) {
      executor = Executors.newFixedThreadPool(2);
    }
    try {
      return executor.invokeAll(tasks);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("boxing")
  public int waitSync(Etag etag, Long timeout, final String database, int replicas) throws TimeoutException {

    if (etag == null) {
      etag = documentStore.getLastEtagHolder().getLastWrittenEtag();
    }

    if (etag == null || Etag.empty().equals(etag)) {
      return replicas; // if the etag is empty, nothing to do
    }

    if (timeout == null) {
      timeout = (long) (60 * 1000);
    }

    final Etag etagToCheck = etag;

    final ServerClient databaseCommands = (ServerClient)
      (database != null ? documentStore.getDatabaseCommands().forDatabase(database) : documentStore.getDatabaseCommands());

    databaseCommands.forceReadFromMaster();

    ReplicationDocumentWithClusterInformation replicationDocument = databaseCommands.executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, ReplicationDocumentWithClusterInformation>() {
      @Override
      public ReplicationDocumentWithClusterInformation apply(OperationMetadata operationMetadata) {
        return databaseCommands.directGetReplicationDestinations(operationMetadata);
      }
    });

    if (replicationDocument == null) {
      return -1;
    }

    List<DestinationAndSourceCollections> destinationsToCheck = new ArrayList<>();
    for (ReplicationDestination destination : replicationDocument.getDestinations()) {
      if (!destination.getDisabled() && !destination.getIgnoredClient()) {
        String url = StringUtils.isEmpty(destination.getClientVisibleUrl()) ? destination.getUrl() : destination.getClientVisibleUrl();
        destinationsToCheck.add(new DestinationAndSourceCollections(RavenUrlExtensions.forDatabase(url, destination.getDatabase()), destination.getSourceCollections()));
      }
    }

    if (destinationsToCheck.isEmpty()) {
      return 0;
    }

    int toCheck = Math.min(replicas, destinationsToCheck.size());

    final CancellationTokenSource cts = new CancellationTokenSource();
    cts.cancelAfter(timeout);

    long started = new Date().getTime();

    IDatabaseCommands sourceCommands = documentStore.getDatabaseCommands().forDatabase(database != null ? database : documentStore.getDefaultDatabase());
    final String sourceUrl = RavenUrlExtensions.forDatabase(documentStore.getUrl(), database != null ? database : documentStore.getDefaultDatabase());
    DatabaseStatistics sourceStatistics = sourceCommands.getStatistics();
    final String sourceDbId = sourceStatistics.getDatabaseId().toString();

    Collection<Callable<Void>> tasks = new ArrayList<>();
    for (final DestinationAndSourceCollections destination: destinationsToCheck) {
      tasks.add(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          waitForReplicationFromServer(destination.getDestination(), sourceUrl, sourceDbId, etagToCheck, destination.getSourceCollections(), cts.getToken());
          return null;
        }
      });
    }


    List<Future<Void>> futures = whenAll(tasks);
    int completedCount = 0;
    int fauledCount = 0;
    for (Future<Void> future: futures) {
      try {
        future.get();
        completedCount++;
      } catch (InterruptedException e) {
        fauledCount++;
      } catch (ExecutionException e) {
        fauledCount++;
      }
    }

    if (fauledCount == 0) {
      return tasks.size();
    }

    if (completedCount >= toCheck) {
      // we have nothing to do here, we replicated to at least the
      // number we had to check, so that is good
      return completedCount;
    }


    // we have either completed (but not enough) or cancelled, meaning timeout
    String msg = String.format("Confirmed that the specified etag %s was replicated to %d of %d servers after %s",
        etag, completedCount, destinationsToCheck.size(), TimeSpan.formatString(new Date().getTime() - started));

    throw new TimeoutException(msg);
  }

  protected void waitForReplicationFromServer(String url, String sourceUrl, String sourceDbId, Etag etag,
                                              String[] sourceCollections, CancellationToken cancellationToken)
          throws InterruptedException {

    while (true) {
      cancellationToken.throwIfCancellationRequested();

      ReplicatedEtagInfo etags = getReplicatedEtagsFor(url, sourceUrl, sourceDbId, sourceCollections);

      boolean replicated = etag.compareTo(etags.getDocumentEtag()) <= 0;
      if (replicated) {
        return;
      }
      Thread.sleep(100);
    }
  }

  private ReplicatedEtagInfo getReplicatedEtagsFor(String destinationUrl, String sourceUrl, String sourceDbId, String[] sourceCollections) {
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(null,
      RavenUrlExtensions.lastReplicatedEtagFor(destinationUrl, sourceUrl, sourceDbId, sourceCollections),
      HttpMethods.GET,
      new OperationCredentials(documentStore.getApiKey()), documentStore.getConventions(), null, null);
    try (HttpJsonRequest httpJsonRequest = documentStore.getJsonRequestFactory().createHttpJsonRequest(createHttpJsonRequestParams)) {
      RavenJToken json = httpJsonRequest.readResponseJson();
      Etag etag = Etag.parse(json.value(String.class, "LastDocumentEtag"));
      log.debug("Received last replicated document Etag %s from server %s", etag, destinationUrl);
      ReplicatedEtagInfo replicatedEtagInfo = new ReplicatedEtagInfo();
      replicatedEtagInfo.setDestionationUrl(destinationUrl);
      replicatedEtagInfo.setDocumentEtag(etag);
      return replicatedEtagInfo;
    }
  }

  @Override
  public void close() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }
}
