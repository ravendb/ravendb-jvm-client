package net.ravendb.client.document;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Closeables;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeTypes;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.exceptions.OperationCancelledException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistExeption;
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
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.profiling.ConcurrentSet;
import net.ravendb.client.utils.CancellationTokenSource;


public class Subscription<T> implements IObservable<T>, AutoCloseable {
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
  private final boolean isStronglyTyped;
  private boolean completed;
  private final long id;
  private boolean disposed;

  private List<EventHandler<VoidArgs>> beforeBatch = new ArrayList<>();
  private List<EventHandler<VoidArgs>> afterBatch = new ArrayList<>();

  private boolean errored;
  private boolean closed;

  Subscription(Class<T> clazz, long id, SubscriptionConnectionOptions options, IDatabaseCommands commands, IDatabaseChanges changes, DocumentConvention conventions, Action0 ensureOpenSubscription) {
    this.id = id;
    this.options = options;
    this.commands = commands;
    this.changes = changes;
    this.conventions = conventions;
    this.ensureOpenSubscription = ensureOpenSubscription;

    isStronglyTyped = !RavenJObject.class.equals(clazz);

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

  private void pullDocuments() {
      while (true) {
        try {
          anySubscriber.waitOne();
        } catch (InterruptedException e) {
          e.printStackTrace(); //TODO: fix me
        }

        cts.getToken().throwIfCancellationRequested();

        boolean pulledDocs = false;
        Etag lastProcessedEtagOnServer = null;
        HttpJsonRequest subscriptionRequest = createPullingRequest();


      }
        /*
+
+                   using (var response = await subscriptionRequest.ExecuteRawResponseAsync().ConfigureAwait(false))
+                   {
+                       await response.AssertNotFailingResponse().ConfigureAwait(false);
+
+                       using (var responseStream = await response.GetResponseStreamWithHttpDecompression().ConfigureAwait(false))
+                       {
+                           cts.Token.ThrowIfCancellationRequested();
+
+                           using (var streamedDocs = new AsyncServerClient.YieldStreamResults(subscriptionRequest, responseStream, customizedEndResult: reader =>
+                           {
+                               if (Equals("LastProcessedEtag", reader.Value) == false)
+                                   return false;
+
+                               lastProcessedEtagOnServer = Etag.Parse(reader.ReadAsString().ResultUnwrap());
+                               return true;
+                           }))
+                           {
+                               while (await streamedDocs.MoveNextAsync().ConfigureAwait(false))
+                               {
+                                   if (pulledDocs == false) // first doc in batch
+                                       BeforeBatch();
+
+                                   pulledDocs = true;
+
+                                   cts.Token.ThrowIfCancellationRequested();
+
+                                   var jsonDoc = streamedDocs.Current;
+                                   foreach (var subscriber in subscribers)
+                                   {
+                                       try
+                                       {
+                                           if (isStronglyTyped)
+                                           {
+                                               var instance = jsonDoc.Deserialize<T>(conventions);
+                                               subscriber.OnNext(instance);
+                                           }
+                                           else
+                                           {
+                                               subscriber.OnNext((T) (object) jsonDoc);
+                                           }
+                                       }
+                                       catch (Exception ex)
+                                       {
+                                           logger.WarnException("Subscriber threw an exception", ex);
+
+                                           if (options.IgnoreSubscribersErrors == false)
+                                           {
+                                               try
+                                               {
+                                                   subscriber.OnError(ex);
+                                               }
+                                               catch (Exception)
+                                               {
+                                                   // can happen if a subscriber doesn't have an onError handler - just ignore it
+                                               }
+                                               IsErrored = true;
+                                               break;
+                                           }
+                                       }
+                                   }
+
+                                   if (IsErrored)
+                                       break;
+                               }
+                           }
+                       }
+                   }
+
+                   if (IsErrored)
+                       break;
+
+                   if (pulledDocs)
+                   {
+                       using (var acknowledgmentRequest = CreateAcknowledgmentRequest(lastProcessedEtagOnServer))
+                       {
+                           try
+                           {
+                               acknowledgmentRequest.ExecuteRequest();
+                           }
+                           catch (Exception)
+                           {
+                               if (acknowledgmentRequest.ResponseStatusCode != HttpStatusCode.RequestTimeout) // ignore acknowledgment timeouts
+                                   throw;
+                           }
+                       }
+
+                       AfterBatch();
+
+                       continue; // try to pull more documents from subscription
+                   }
+
+                   while (newDocuments.WaitOne(options.ClientAliveNotificationInterval) == false)
+                   {
+                       using (var clientAliveRequest = CreateClientAliveRequest())
+                       {
+                           clientAliveRequest.ExecuteRequest();
+                       }
+                   }
         */


  }

   private Thread startPullingDocs() {
     Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          pullDocuments();
        } catch (Exception ex) {
          if (cts.getToken().isCancellationRequested()) {
            return;
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

  //TODO: it should return task!
  private void restartPullingTask() {
    try {
      Thread.sleep(15000);
      ensureOpenSubscription.apply();
    } catch (Exception ex) {
      if (ex instanceof SubscriptionInUseException || ex instanceof SubscriptionDoesNotExistExeption) {
        // another client has connected to the subscription or it has been deleted meanwhile - we cannot open it so we need to finish
        onCompletedNotification();
        return;
      }

      restartPullingTask();
      return;
    }
//TODO: startPullingTask = startPullingDocs();
    startPullingDocs(); //TODO: delete me!
  }

  private void startWatchingDocs() {
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

  @Override
  public Closeable subscribe(final IObserver<T> observer) {
    if (errored) {
      throw new IllegalStateException("Subscription encountered errors and stopped. Cannot add any subscriber.");
    }

    if (subscribers.add(observer)) {
      if (subscribers.size() == 1) {
        anySubscriber.set();
      }
    }

    return new Closeable() {
      @Override
      public void close() throws IOException {
        subscribers.remove(observer);
        if (subscribers.isEmpty()) {
          anySubscriber.reset();
        }
      }
    };
  }

    private HttpJsonRequest createAcknowledgmentRequest(Etag lastProcessedEtag) {
      return commands.createRequest(HttpMethods.POST,
        String.format("/subscriptions/acknowledgeBatch?id=%d&lastEtag=%s&connection=%s", id, lastProcessedEtag, options.getConnectionId()));
    }

    private HttpJsonRequest createPullingRequest() {
      return commands.createRequest(HttpMethods.GET,
        String.format("/subscriptions/pull?id=%d&connection=%s", id, options.getConnectionId()));
    }

    private HttpJsonRequest createClientAliveRequest() {
      return commands.createRequest(HttpMethods.PATCH,
        String.format("/subscriptions/client-alive?id=%d&connection=%s", id, options.getConnectionId()));
    }

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

      if (changes instanceof AutoCloseable) {
        Closeable autocloseableChanges = (Closeable) changes;
        Closeables.closeQuietly(autocloseableChanges);
      }

      cts.cancel();

      newDocuments.set();
      anySubscriber.set();

      try {
        startPullingTask.join();
      } catch (OperationCancelledException e) {
        //ignore
      } catch (Exception e ) {
        e.printStackTrace();
        //TODO: throw;
      }
      if (!closed) {
        closeSubscription();
      }
    }

    private void closeSubscription() {
      HttpJsonRequest closeRequest = createCloseRequest();
      closeRequest.executeRequest();
      closed = true;
    }

    public Thread getStartPullingTask() {
      return startPullingTask;
    }

    @Override
    public IObservable<T> where(Predicate<T> predicate) {
      throw new IllegalStateException("Where is not supported in subscriptions!");
    }

}
