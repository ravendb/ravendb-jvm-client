package net.ravendb.client.document;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.DatabaseStatistics;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.replication.ReplicatedEtagInfo;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDocument;
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

    ReplicationDocument replicationDocument = databaseCommands.executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, ReplicationDocument>() {
      @Override
      public ReplicationDocument apply(OperationMetadata operationMetadata) {
        return databaseCommands.directGetReplicationDestinations(operationMetadata);
      }
    });

    if (replicationDocument == null) {
      return -1;
    }

    List<String> destinationsToCheck = new ArrayList<>();
    for (ReplicationDestination destination : replicationDocument.getDestinations()) {
      if (!destination.getDisabled() && !destination.getIgnoredClient()) {
        String url = StringUtils.isEmpty(destination.getClientVisibleUrl()) ? destination.getUrl() : destination.getClientVisibleUrl();
        destinationsToCheck.add(RavenUrlExtensions.forDatabase(url, destination.getDatabase()));
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

    final ReplicatedEtagInfo[] latestEtags = new ReplicatedEtagInfo[destinationsToCheck.size()];

    Collection<Callable<Void>> tasks = new ArrayList<>();

    int i = 0;
    for (final String destination: destinationsToCheck) {
      final Integer index = i;
      tasks.add(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          waitForReplicationFromServer(destination, sourceUrl, sourceDbId, etagToCheck, latestEtags, index, cts.getToken());
          return null;
        }
      });
      i++;
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
    String msg = String.format("Could only confirm that the specified Etag %s was replicated to %d of %d servers after %s. Details: %s",
        etag, completedCount, destinationsToCheck.size(), TimeSpan.formatString(new Date().getTime() - started), StringUtils.join(latestEtags));

    throw new TimeoutException(msg);
  }

  protected void waitForReplicationFromServer(String url, String sourceUrl, String sourceDbId, Etag etag,
                                              ReplicatedEtagInfo[] latestEtags, int index,
    CancellationToken cancellationToken) throws InterruptedException {

    while (true) {
      cancellationToken.throwIfCancellationRequested();
      try {
        ReplicatedEtagInfo etags = getReplicatedEtagsFor(url, sourceUrl, sourceDbId);

        latestEtags[index] = etags;

        boolean replicated = etag.compareTo(etags.getDocumentEtag()) <= 0;
        if (replicated) {
          return;
        }
      } catch (Exception e) {
        log.debugException("Failed to get replicated etags for " + sourceUrl, e);
      }
      Thread.sleep(100);
    }
  }

  private ReplicatedEtagInfo getReplicatedEtagsFor(String destinationUrl, String sourceUrl, String sourceDbId) {
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(null,
      RavenUrlExtensions.lastReplicatedEtagFor(destinationUrl, sourceUrl, sourceDbId),
      HttpMethods.GET,
      new RavenJObject(), new OperationCredentials(documentStore.getApiKey()), documentStore.getConventions());
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
