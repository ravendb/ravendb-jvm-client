package net.ravendb.client.document;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.codehaus.jackson.JsonParser;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventArgs;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.DocumentChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeTypes;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.exceptions.OperationCancelledException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionClosedException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistExeption;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionInUseException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.util.AutoResetEvent;
import net.ravendb.abstractions.util.ManualResetEvent;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.changes.IObservable;
import net.ravendb.client.changes.IObserver;
import net.ravendb.client.changes.ObserverAdapter;
import net.ravendb.client.changes.RemoteDatabaseChanges;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.RavenJObjectIterator;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.profiling.ConcurrentSet;
import net.ravendb.client.extensions.HttpJsonRequestExtension;
import net.ravendb.client.utils.CancellationTokenSource;

import com.google.common.io.Closeables;


public class Subscription<T> implements IObservable<T>, CleanCloseable {
  protected static final ILog logger = LogManager.getCurrentClassLogger();

  protected AutoResetEvent newDocuments = new AutoResetEvent(false);
  private ManualResetEvent anySubscriber = new ManualResetEvent(false);

  private final IDatabaseCommands commands;
  private final IDatabaseChanges changes;
  private final DocumentConvention conventions;
  private final Action0 ensureOpenSubscription;
  private final ConcurrentSet<IObserver<T>> subscribers = new ConcurrentSet<>();
  private final SubscriptionConnectionOptions options;
  private final CancellationTokenSource cts = new CancellationTokenSource();
  private GenerateEntityIdOnTheClient generateEntityIdOnTheClient;
  private final boolean isStronglyTyped;
  private boolean completed;
  private final long id;
  private final Class<T> clazz;
  private boolean disposed;

  private EventHandler<VoidArgs> eventHandler;

  private List<EventHandler<VoidArgs>> beforeBatch = new ArrayList<>();
  private List<EventHandler<VoidArgs>> afterBatch = new ArrayList<>();

  private boolean errored;
  private boolean closed;
  private boolean firstConnection = true;

  Subscription(Class<T> clazz, long id, final String database, SubscriptionConnectionOptions options, final IDatabaseCommands commands, IDatabaseChanges changes, final DocumentConvention conventions, Action0 ensureOpenSubscription) {
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

    startWatchingDocs();
    startPullingTask = startPullingDocs();
  }

  public void addBeforeBatchHandler(EventHandler<VoidArgs> handler) {
    beforeBatch.add(handler);
  }

  public void removeBeforeBatchHandler(EventHandler<VoidArgs> handler) {
    beforeBatch.remove(handler);
  }

  public void addAfterBatchHandler(EventHandler<VoidArgs> handler) {
    afterBatch.add(handler);
  }

  public void removeAfterBatchHandler(EventHandler<VoidArgs> handler) {
    afterBatch.remove(handler);
  }

  /**
   * It indicates if the subscription is in errored state.
   */
  public boolean isErrored() {
    return errored;
  }

  /**
   * It determines if the subscription is closed.
   */
  public boolean isClosed() {
    return closed;
  }

  private Thread startPullingTask;

  private Closeable putDocumentsObserver;
  private Closeable endedBulkInsertsObserver;
  private Etag lastProcessedEtagOnServer = null;

  @SuppressWarnings({"boxing", "unchecked"})
  private void pullDocuments() throws IOException, InterruptedException {
    while (true) {
      anySubscriber.waitOne();

      cts.getToken().throwIfCancellationRequested();

      boolean pulledDocs = false;
      lastProcessedEtagOnServer = null;
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
                  errored = true;
                  return false;
                }
                lastProcessedEtagOnServer = Etag.parse(reader.getText());
                return true;
              } catch (IOException e ) {
                errored = true;
                return false;
              }
            }
          })) {
            while (streamedDocs.hasNext()) {
              if (pulledDocs == false) {
                EventHelper.invoke(beforeBatch, this, EventArgs.EMPTY);
              }
              pulledDocs = true;

              cts.getToken().throwIfCancellationRequested();

              RavenJObject jsonDoc = streamedDocs.next();
              T instance = null;
              for (IObserver<T> subscriber : subscribers) {
                try {
                  if (isStronglyTyped) {
                    if (instance == null) {
                      instance = conventions.createSerializer().deserialize(jsonDoc.toString(), clazz);
                      String docId = jsonDoc.get(Constants.METADATA).value(String.class, "@id");
                      if (StringUtils.isNotEmpty(docId)) {
                        generateEntityIdOnTheClient.trySetIdentity(instance, docId);
                      }
                    }
                    subscriber.onNext(instance);
                  } else {
                    subscriber.onNext((T) jsonDoc);
                  }
                } catch (Exception ex) {
                  logger.warnException("Subscriber threw an exception", ex);
                  if (options.isIgnoreSubscribersErrors() == false) {
                    errored = true;
                    try {
                      subscriber.onError(ex);
                    } catch (Exception e) {
                      // can happen if a subscriber doesn't have an onError handler - just ignore it
                    }
                    break;
                  }
                }
              }
              if (errored) {
                break;
              }
            }
          }
        }

        if (errored) {
          break;
        }
        if (pulledDocs) {
          try (HttpJsonRequest acknowledgmentRequest = createAcknowledgmentRequest(lastProcessedEtagOnServer)) {
            try {
              acknowledgmentRequest.executeRequest();
            } catch (Exception e) {
              if (acknowledgmentRequest.getResponseStatusCode() != HttpStatus.SC_REQUEST_TIMEOUT) // ignore acknowledgment timeouts
                throw e;
            }
          }

          EventHelper.invoke(afterBatch, this, EventArgs.EMPTY);
          continue; // try to pull more documents from subscription
        }

        while (newDocuments.waitOne(options.getClientAliveNotificationInterval(), TimeUnit.MILLISECONDS) == false) {
          try (HttpJsonRequest clientAliveRequest = createClientAliveRequest()) {
            clientAliveRequest.executeRequest();
          }
        }
      }
    }
  }

   private Thread startPullingDocs() {
     Runnable runnable = new Runnable() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void run() {
        try {
          pullDocuments();
        } catch (Exception ex) {
          if (cts.getToken().isCancellationRequested()) {
            return;
          }

          if (ex instanceof SubscriptionException) {
            SubscriptionException subscriptionEx = DocumentSubscriptions.tryGetSubscriptionException(ex);
            if (subscriptionEx != null && subscriptionEx instanceof SubscriptionClosedException) {
              // someone forced us to drop the connection by calling subscriptions.release
              onCompletedNotification();
              closed = true;
              return;
            }
          }

          restartPullingTask();
        }

        if (errored) {
          onCompletedNotification();

          try {
            closeSubscription();
          } catch (Exception e) {
            logger.warnException("Exception happened during an attempt to close subscription after it becomes faulted", e);
          }
        }
      }
    };

    Thread pullingThread = new Thread(runnable, "Subscription pulling thread");
    pullingThread.start();
    return pullingThread;
  }

  private void restartPullingTask() {
    boolean connected = false;
    while (!connected) {
      try {
        Thread.sleep(15000);
        ensureOpenSubscription.apply();
        connected = true;
      } catch (Exception ex) {
        if (ex instanceof SubscriptionInUseException || ex instanceof SubscriptionDoesNotExistExeption) {
          // another client has connected to the subscription or it has been deleted meanwhile - we cannot open it so we need to finish
          onCompletedNotification();
          return;
        }
      }
    }
    startPullingTask = startPullingDocs();
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
    if (errored) {
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
        String.format("/subscriptions/pull?id=%d&connection=%s", id, options.getConnectionId()));
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

      if (changes instanceof CleanCloseable) {
        Closeable closeableChanges = (Closeable) changes;
        Closeables.closeQuietly(closeableChanges);
      }

      cts.cancel();

      newDocuments.set();
      anySubscriber.set();

      if (eventHandler != null) {
        changes.removeConnectionStatusChanges(eventHandler);
      }

      try {
        startPullingTask.join();
      } catch (OperationCancelledException e) {
        //ignore
      } catch (InterruptedException e) {
        // ignore
      }
      if (!closed) {
        closeSubscription();
      }
    }

    private void closeSubscription() {
      try (HttpJsonRequest closeRequest = createCloseRequest()) {
        closeRequest.executeRequest();
        closed = true;
      }
    }

    public Thread getStartPullingTask() {
      return startPullingTask;
    }

    @Override
    public IObservable<T> where(Predicate<T> predicate) {
      throw new IllegalStateException("Where is not supported in subscriptions!");
    }

}
