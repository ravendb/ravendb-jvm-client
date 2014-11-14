package net.ravendb.client.document;

import static net.ravendb.client.connection.RavenUrlExtensions.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;

import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.replication.ReplicatedEtagInfo;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDocument;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.RavenUrlExtensions;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.utils.CancellationTokenSource;
import net.ravendb.client.utils.TimeSpan;
import net.ravendb.client.utils.CancellationTokenSource.CancellationToken;


public class ReplicationBehavior implements AutoCloseable {
  private final DocumentStore documentStore;

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

  public int waitSync(Etag etag, Long timeout, final String database, int replicas) throws TimeoutException {

    if (etag == null) {
      etag = documentStore.getLastEtagHolder().getLastWrittenEtag();
    }

    if (etag == null || Etag.empty().equals(etag)) {
      return replicas; // if the etag is empty, nothing to do
    }

    if (timeout == null) {
      timeout = (long) (30 * 1000);
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

    Collection<Callable<Void>> tasks = new ArrayList<>();
    for (final String destination: destinationsToCheck) {
      tasks.add(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          waitForReplicationFromServer(destination, database, etagToCheck, cts.getToken());
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

  protected void waitForReplicationFromServer(String url, String database, Etag etag,
    CancellationToken cancellationToken) throws InterruptedException {

    while (true) {
      cancellationToken.throwIfCancellationRequested();

      ReplicatedEtagInfo etags = getReplicatedEtagsFor(url, database);

      boolean replicated = etag.compareTo(etags.getDocumentEtag()) <= 0 || etag.compareTo(etags.getAttachmentEtag()) <= 0;
      if (replicated) {
        return;
      }
      Thread.sleep(100);
    }
  }



  private ReplicatedEtagInfo getReplicatedEtagsFor(String destinationUrl, String database) {
    String databaseUrl = RavenUrlExtensions.forDatabase(documentStore.getUrl(), database != null ? database : documentStore.getDefaultDatabase());
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(null,
      RavenUrlExtensions.lastReplicatedEtagFor(destinationUrl, databaseUrl),
      HttpMethods.GET,
      new RavenJObject(), new OperationCredentials(documentStore.getApiKey()), documentStore.getConventions());
    HttpJsonRequest httpJsonRequest = documentStore.getJsonRequestFactory().createHttpJsonRequest(createHttpJsonRequestParams);
    RavenJToken json = httpJsonRequest.readResponseJson();

    ReplicatedEtagInfo replicatedEtagInfo = new ReplicatedEtagInfo();
    replicatedEtagInfo.setDestionationUrl(destinationUrl);
    replicatedEtagInfo.setDocumentEtag(Etag.parse(json.value(String.class, "LastDocumentEtag")));
    replicatedEtagInfo.setAttachmentEtag(Etag.parse(json.value(String.class, "LastAttachmentEtag")));
    return replicatedEtagInfo;
  }

  @Override
  public void close() throws Exception {
    if (executor != null) {
      executor.shutdownNow();
    }
  }
}
