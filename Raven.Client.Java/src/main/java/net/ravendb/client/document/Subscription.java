package net.ravendb.client.document;

import com.google.common.io.Closeables;
import net.ravendb.abstractions.basic.*;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.data.*;
import net.ravendb.abstractions.exceptions.OperationCancelledException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionClosedException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionInUseException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.util.AutoResetEvent;
import net.ravendb.abstractions.util.ManualResetEvent;
import net.ravendb.client.changes.*;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.RavenJObjectIterator;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.profiling.ConcurrentSet;
import net.ravendb.client.extensions.HttpJsonRequestExtension;
import net.ravendb.client.utils.CancellationTokenSource;
import net.ravendb.client.utils.Observers;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.codehaus.jackson.JsonParser;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class Subscription<T> implements IObservable<T>, CleanCloseable {

  private final static Object END_OF_COLLECTION_MARKER = new Object();

  private final ExecutorService executorService = Executors.newFixedThreadPool(3);

  protected static final ILog logger = LogManager.getCurrentClassLogger();

  private final Class<T> clazz;

  private final AutoResetEvent newDocuments = new AutoResetEvent(false);
  private final ManualResetEvent anySubscriber = new ManualResetEvent(false);
  private final IDatabaseCommands commands;
  private final IDatabaseChanges changes;
  private final DocumentConvention conventions;
  private final Action0 ensureOpenSubscription;
  private final ConcurrentSet<IObserver<T>> subscribers = new ConcurrentSet<>();
  private final SubscriptionConnectionOptions options;
  private final CancellationTokenSource cts = new CancellationTokenSource();
  private GenerateEntityIdOnTheClient generateEntityIdOnTheClient;
  private final boolean isStronglyTyped;
  private final long id;
  private Future<?> pullingTask;
  private Future<?> startPullingTask;
  private CleanCloseable putDocumentsObserver;
  private CleanCloseable endedBulkInsertsObserver;
  private CleanCloseable dataSubscriptionReleasedObserver;
  private boolean completed;
  private boolean disposed;
  private boolean firstConnection = true;

  private List<EventHandler<VoidArgs>> beforeBatch = new ArrayList<>();
  private List<EventHandler<DocumentProcessedEventArgs>> afterBatch = new ArrayList<>();
  private List<EventHandler<VoidArgs>> beforeAcknowledgment = new ArrayList<>();
  private List<EventHandler<LastProcessedEtagEventArgs>> afterAcknowledgment = new ArrayList<>();


  private EventHandler<VoidArgs> eventHandler;

  private boolean isErroredBecauseOfSubscriber;
  private Exception lastSubscriberException;
  private Throwable subscriptionConnectionException;
  private boolean connectionClosed;

  Subscription(Class<T> clazz, long id, final String database, SubscriptionConnectionOptions options,
               final IDatabaseCommands commands, IDatabaseChanges changes, final DocumentConvention conventions,
               final boolean open, Action0 ensureOpenSubscription) {
    this.clazz = clazz;
    this.id = id;
    this.options = options;
    this.commands = commands;
    this.changes = changes;
    this.conventions = conventions;
    this.ensureOpenSubscription = ensureOpenSubscription;

    if (!RavenJObject.class.equals(clazz)) {
      isStronglyTyped = true;
      generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(conventions, new Function1<Object, String>() {
        @Override
        public String apply(Object entity) {
          return conventions.generateDocumentKey(database, commands, entity);
        }
      });
    } else {
      isStronglyTyped = false;
    }

    if (open) {
      start();
    } else {
      if (options.getStrategy() != SubscriptionOpeningStrategy.WAIT_FOR_FREE) {
        throw new IllegalStateException("Subscription isn't open while its opening strategy is: " + options.getStrategy());
      }
    }

    if (options.getStrategy() == SubscriptionOpeningStrategy.WAIT_FOR_FREE) {
      waitForSubscriptionReleased();
    }
  }

  private void start() {
    startWatchingDocs();
    startPullingTask = startPullingDocs();
  }

  @SuppressWarnings({"boxing", "unchecked"})
  private Future<?> pullDocuments() {
    return executorService.submit(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        try {
          Etag lastProcessedEtagOnClient = null;

          while (true) {
            anySubscriber.waitOne();

            cts.getToken().throwIfCancellationRequested();

            boolean pulledDocs = false;
            final Reference<Etag> lastProcessedEtagOnServerRef = new Reference<>();
            final Reference<Integer> processedDocsRef = new Reference<>(0);

            final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(options.getBatchOptions().getMaxDocCount() + 1);
            Future<?> processingTask = null;

            try (HttpJsonRequest subscriptionRequest = createPullingRequest()) {
              try (CloseableHttpResponse response = subscriptionRequest.executeRawResponse()) {
                HttpJsonRequestExtension.assertNotFailingResponse(response);

                try (RavenJObjectIterator streamedDocs = ServerClient.yieldStreamResults(response, 0, Integer.MAX_VALUE, null, new Function1<JsonParser, Boolean>() {
                  @SuppressWarnings({"synthetic-access"})
                  @Override
                  public Boolean apply(JsonParser reader) {
                    try {
                      if (!"LastProcessedEtag".equals(reader.getText())) {
                        return false;
                      }
                      if (reader.nextToken() == null) {
                        return false;
                      }
                      lastProcessedEtagOnServerRef.value = Etag.parse(reader.getText());
                      return true;
                    } catch (IOException e) {
                      return false;
                    }
                  }
                })) {
                  while (streamedDocs.hasNext()) {
                    if (pulledDocs == false) {
                      EventHelper.invoke(beforeBatch, this, EventArgs.EMPTY);

                      processingTask = executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                          T doc;
                          try {
                            while (true) {
                              Object takenObject = queue.take();

                              if (END_OF_COLLECTION_MARKER == takenObject) {
                                break;
                              }

                              doc = (T) takenObject;
                              cts.getToken().throwIfCancellationRequested();

                              for (IObserver<T> subscriber : subscribers) {
                                try {
                                  subscriber.onNext(doc);
                                } catch (Exception ex) {
                                  logger.warnException("Subscriber threw an exception", ex);
                                  if (options.isIgnoreSubscribersErrors() == false) {
                                    isErroredBecauseOfSubscriber = true;
                                    lastSubscriberException = ex;
                                    try {
                                      subscriber.onError(ex);
                                    } catch (Exception e) {
                                      // can happen if a subscriber doesn't have an onError handler - just ignore it
                                    }
                                    break;
                                  }
                                }
                              }

                              if (isErroredBecauseOfSubscriber) {
                                break;
                              }

                              processedDocsRef.value++;
                            }
                          } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                          }
                        }
                      });
                    }
                    pulledDocs = true;

                    cts.getToken().throwIfCancellationRequested();

                    RavenJObject jsonDoc = streamedDocs.next();

                    if (isStronglyTyped) {
                      T instance = conventions.createSerializer().deserialize(jsonDoc.toString(), clazz);
                      String docId = jsonDoc.get(Constants.METADATA).value(String.class, "@id");

                      if (StringUtils.isNotEmpty(docId)) {
                        generateEntityIdOnTheClient.trySetIdentity(instance, docId);
                      }

                      queue.add(instance);
                    } else {
                      queue.add((T) jsonDoc);
                    }

                    if (isErroredBecauseOfSubscriber) {
                      break;
                    }
                  }
                }
              }

              queue.add(END_OF_COLLECTION_MARKER);

              if (processingTask != null) {
                processingTask.get();
              }

              if (isErroredBecauseOfSubscriber) {
                break;
              }

              if (lastProcessedEtagOnServerRef.value != null) {

                // This is an acknowledge when the server returns documents to the subscriber.
                if (pulledDocs) {
                  EventHelper.invoke(beforeAcknowledgment, this, EventArgs.EMPTY);
                  acknowledgeBatchToServer(lastProcessedEtagOnServerRef.value);
                  EventHelper.invoke(afterAcknowledgment, this, new LastProcessedEtagEventArgs(lastProcessedEtagOnServerRef.value));

                  EventHelper.invoke(afterBatch, this, new DocumentProcessedEventArgs(processedDocsRef.value));
                  continue; // try to pull more documents from subscription
                } else {
                  if (!lastProcessedEtagOnServerRef.value.equals(lastProcessedEtagOnClient)) {
                    // This is a silent acknowledge, this can happen because there was no documents in range
                    // to be accessible in the time available. This condition can happen when documents must match
                    // a set of conditions to be eligible.
                    acknowledgeBatchToServer(lastProcessedEtagOnServerRef.value);

                    lastProcessedEtagOnClient = lastProcessedEtagOnServerRef.value;

                    continue; // try to pull more documents from subscription
                  }
                }
              }

              while (newDocuments.waitOne(options.getClientAliveNotificationInterval(), TimeUnit.MILLISECONDS) == false) {
                try (HttpJsonRequest clientAliveRequest = createClientAliveRequest()) {
                  clientAliveRequest.executeRequest();
                }
              }
            }
          }
        } catch (InterruptedException | IOException e) {
          throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
          SubscriptionException subscriptionException = DocumentSubscriptions.tryGetSubscriptionException(e);
          if (subscriptionException != null) {
            throw subscriptionException;
          }
          throw e;
        }
        return null;
      }
    });
  }

  private void acknowledgeBatchToServer(Etag lastProcessedEtagOnServer) {
    try (HttpJsonRequest acknowledgmentRequest = createAcknowledgmentRequest(lastProcessedEtagOnServer)) {
      try {
        acknowledgmentRequest.executeRequest();
      } catch (Exception e) {
        if (acknowledgmentRequest.getResponseStatusCode() != HttpStatus.SC_REQUEST_TIMEOUT) // ignore acknowledgment timeouts
          throw e;
      }
    }
  }

   private Future<?> startPullingDocs() {
     return executorService.submit(new Runnable() {
      @Override
      public void run() {
        subscriptionConnectionException = null;
        pullingTask = pullDocuments();

        try {
          pullingTask.get();
        } catch (Exception ex) {
          if (cts.getToken().isCancellationRequested()) {
            return;
          }

          logger.warn(String.format("Subscription #%d. Pulling task threw the following exception: ", id), ex);

          if (ex instanceof ExecutionException && tryHandleRejectedConnection(ex.getCause(), false)) {
            logger.debug(String.format("Subscription #%d. Stopping the connection '%s'", id, options.getConnectionId()));
            return;
          }

          restartPullingTask();
        }

        if (isErroredBecauseOfSubscriber) {
          try {
            startPullingTask = null; // prevent from calling Wait() on this in Dispose because we are already inside this task
            close();
          } catch (Exception e) {
            logger.warnException("Exception happened during an attempt to close subscription after it had become faulted", e);
          }
        }
      }
    });
  }

  private Future<?> restartPullingTask() {
    return executorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(options.getTimeToWaitBeforeConnectionRetry());
          ensureOpenSubscription.apply();
        } catch (Exception ex) {
          if (tryHandleRejectedConnection(ex, true)) {
            return;
          }

          restartPullingTask();
          return ;
        }

        startPullingTask = startPullingDocs();
      }
    });
  }

  private boolean tryHandleRejectedConnection(Throwable ex, boolean handleClosedException) {
    subscriptionConnectionException = ex;
    if (ex instanceof SubscriptionInUseException ||  // another client has connected to the subscription
            ex instanceof  SubscriptionDoesNotExistException ||  // subscription has been deleted meanwhile
            (handleClosedException && ex instanceof SubscriptionClosedException)) { // someone forced us to drop the connection by calling Subscriptions.Release
      connectionClosed = true;
      startPullingTask = null; // prevent from calling Wait() on this in close because we can be already inside this task
      pullingTask = null; // prevent from calling Wait() on this in close because we can be already inside this task
      close();
      return true;
    }
    return false;
  }

  private void startWatchingDocs() {
    eventHandler = new EventHandler<VoidArgs>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void handle(Object sender, VoidArgs event) {
        changesApiConnectionChanged(sender, event);
      }
    };
    changes.addConnectionStatusChanged(eventHandler);

    putDocumentsObserver = changes.forAllDocuments().subscribe(new ObserverAdapter<DocumentChangeNotification>() {
      @Override
      public void onNext(DocumentChangeNotification notification) {
        if (DocumentChangeTypes.PUT.equals(notification.getType()) && !notification.getId().startsWith("Raven/")) {
          newDocuments.set();
        }
      }
    });

    endedBulkInsertsObserver = changes.forBulkInsert().subscribe(new ObserverAdapter<BulkInsertChangeNotification>() {
      @Override
      public void onNext(BulkInsertChangeNotification notification) {
        if (DocumentChangeTypes.BULK_INSERT_ENDED.equals(notification.getType())) {
          newDocuments.set();
        }
      }
    });
  }

  private void waitForSubscriptionReleased() {
    IObservable<DataSubscriptionChangeNotification> dataSubscriptionObservable = changes.forDataSubscription(id);
    dataSubscriptionReleasedObserver = dataSubscriptionObservable.subscribe(new Observers.ActionBasedObserver<>(new Action1<DataSubscriptionChangeNotification>() {
      @Override
      public void apply(DataSubscriptionChangeNotification notification) {
        if (notification.getType() == DataSubscriptionChangeTypes.SUBSCRIPTION_RELEASED) {
          try {
            ensureOpenSubscription.apply();
          } catch (Exception e) {
            return ;
          }

          // succeeded in opening the subscription

          // no longer need to be notified about subscription status changes
          dataSubscriptionReleasedObserver.close();
          dataSubscriptionReleasedObserver = null;

          // start standard stuff
          start();
        }
      }
    }));
  }

  @SuppressWarnings("unused")
  private void changesApiConnectionChanged(Object sender, EventArgs e) {
    if (firstConnection) {
      firstConnection = false;
      return;
    }

    RemoteDatabaseChanges changesApi = (RemoteDatabaseChanges) sender;
    if (changesApi.isConnected()){
      newDocuments.set();
    }
  }

  @Override
  public CleanCloseable subscribe(final IObserver<T> observer) {
    if (isErroredBecauseOfSubscriber) {
      throw new IllegalStateException("Subscription encountered errors and stopped. Cannot add any subscriber.");
    }

    if (subscribers.add(observer)) {
      if (subscribers.size() == 1) {
        anySubscriber.set();
      }
    }

    return new CleanCloseable() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void close() {
        subscribers.remove(observer);
        if (subscribers.isEmpty()) {
          anySubscriber.reset();
        }
      }
    };
  }

    @SuppressWarnings("boxing")
    private HttpJsonRequest createAcknowledgmentRequest(Etag lastProcessedEtag) {
      return commands.createRequest(HttpMethods.POST,
        String.format("/subscriptions/acknowledgeBatch?id=%d&lastEtag=%s&connection=%s", id, lastProcessedEtag, options.getConnectionId()));
    }

    @SuppressWarnings("boxing")
    private HttpJsonRequest createPullingRequest() {
      return commands.createRequest(HttpMethods.GET,
        String.format("/subscriptions/pull?id=%d&connection=%s", id, options.getConnectionId()), false, false, options.getPullingRequestTimeout());
    }

    @SuppressWarnings("boxing")
    private HttpJsonRequest createClientAliveRequest() {
      return commands.createRequest(HttpMethods.PATCH,
        String.format("/subscriptions/client-alive?id=%d&connection=%s", id, options.getConnectionId()));
    }

    @SuppressWarnings("boxing")
    private HttpJsonRequest createCloseRequest() {
      return commands.createRequest(HttpMethods.POST,
        String.format("/subscriptions/close?id=%d&connection=%s", id, options.getConnectionId()));
    }

    private void onCompletedNotification() {
      if (completed) {
        return;
      }

      for (IObserver<T> subscriber: subscribers) {
        subscriber.onCompleted();
      }
      completed = true;
    }

    @Override
    public void close() {
      if (disposed) {
        return;
      }
      disposed = true;

      onCompletedNotification();

      subscribers.clear();

      Closeables.closeQuietly(putDocumentsObserver);

      Closeables.closeQuietly(endedBulkInsertsObserver);

      Closeables.closeQuietly(dataSubscriptionReleasedObserver);

      if (changes instanceof CleanCloseable) { //TODO delete this?
        Closeable closeableChanges = (Closeable) changes;
        Closeables.closeQuietly(closeableChanges);
      }

      cts.cancel();

      newDocuments.set();
      anySubscriber.set();

      if (eventHandler != null) {
        changes.removeConnectionStatusChanges(eventHandler);
      }

      Future[] futures = new Future[] { pullingTask, startPullingTask };

      for (int i = 0; i < futures.length; i++) {
        try {
          Future future = futures[i];
          if (future != null) {
            future.get();
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        } catch (ExecutionException e ) {
          if (!(e.getCause() instanceof OperationCancelledException)) {
            throw new RuntimeException(e);
          }
        }
      }

      executorService.shutdown(); //TODO: make sure this invocation will throw is any task in queue will throw
      // TODO: verify if it stopping underlaying threads
      // TODO: filter for operation canceled exception and don't rethrow if such

      if (!connectionClosed) {
        closeSubscription();
      }
    }

    private void closeSubscription() {
      try (HttpJsonRequest closeRequest = createCloseRequest()) {
        closeRequest.executeRequest();
        connectionClosed = true;
      }
    }

    @Override
    public IObservable<T> where(Predicate<T> predicate) {
      throw new IllegalStateException("Where is not supported in subscriptions!");
    }

  public static class DocumentProcessedEventArgs extends EventArgs {
    public DocumentProcessedEventArgs(int documentsProcessed) {
      this.documentsProcessed = documentsProcessed;
    }

    private final int documentsProcessed;

    public int getDocumentsProcessed() {
      return documentsProcessed;
    }
  }

  public static class LastProcessedEtagEventArgs extends EventArgs {
    private final Etag lastProcessedEtag;

    public LastProcessedEtagEventArgs(Etag lastProcessedEtag) {
      this.lastProcessedEtag = lastProcessedEtag;
    }

    public Etag getLastProcessedEtag() {
      return lastProcessedEtag;
    }
  }


  public void addBeforeBatchHandler(EventHandler<VoidArgs> handler) {
    beforeBatch.add(handler);
  }

  public void removeBeforeBatchHandler(EventHandler<VoidArgs> handler) {
    beforeBatch.remove(handler);
  }

  public void addAfterBatchHandler(EventHandler<DocumentProcessedEventArgs> handler) {
    afterBatch.add(handler);
  }

  public void removeAfterBatchHandler(EventHandler<DocumentProcessedEventArgs> handler) {
    afterBatch.remove(handler);
  }

  public void addBeforeAcknowledgmentHandler(EventHandler<VoidArgs> handler) {
    beforeAcknowledgment.add(handler);
  }

  public void removeBeforeAcknowledgmentHandler(EventHandler<VoidArgs> handler) {
    beforeAcknowledgment.remove(handler);
  }

  public void addAfterAcknowledgmentHandler(EventHandler<LastProcessedEtagEventArgs> handler) {
    afterAcknowledgment.add(handler);
  }

  public void removeAfterAcknowledgmentHandler(EventHandler<LastProcessedEtagEventArgs> handler) {
    afterAcknowledgment.remove(handler);
  }

  /**
   * It determines if the subscription is closed.
   */
  public boolean isConnectionClosed() {
    return connectionClosed;
  }

  /**
   * @return It indicates if the subscription is in errored state because one of subscribers threw an exception.
   */
  public boolean isErroredBecauseOfSubscriber() {
    return isErroredBecauseOfSubscriber;
  }

  /**
   *  The last subscription connection exception.
   */
  public Throwable getSubscriptionConnectionException() {
    return subscriptionConnectionException;
  }

  /**
   * The last exception thrown by one of subscribers.
   */
  public Throwable getLastSubscriberException() {
    return lastSubscriberException;
  }


}
