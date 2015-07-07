package net.ravendb.client.changes;

import net.ravendb.abstractions.basic.*;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.util.AtomicDictionary;
import net.ravendb.abstractions.util.Base62Util;
import net.ravendb.client.ConventionBase;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.HttpConnectionHelper;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.utils.UrlUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeoutException;


public abstract class RemoteChangesClientBase<TChangesApi extends IConnectableChanges, TConnectionState extends IChangesConnectionState, TConventions extends ConventionBase>
  implements CleanCloseable, IObserver<String>, IConnectableChanges {
  protected static final ILog logger = LogManager.getCurrentClassLogger();

  private Class<TConnectionState> connectionStateClass;

  private Timer clientSideHeartbeatTimer;

  private final String url;
  private OperationCredentials credentials;
  private final HttpJsonRequestFactory jsonRequestFactory;

  private final Action0 onDispose;

  private Closeable connection;
  private Date lastHeartbeat = new Date();

  private static int connectionCounter;
  private final String id;

  // This is the StateCounters, it is not related to the counters database
  protected final AtomicDictionary<TConnectionState> counters = new AtomicDictionary<>(String.CASE_INSENSITIVE_ORDER);

  private boolean connected;

  private List<EventHandler<VoidArgs>> connectionStatusChanged;

  private volatile boolean disposed;

  protected TConventions conventions;


  @SuppressWarnings("rawtypes")
  protected RemoteChangesClientBase(Class<TConnectionState> tConnectionStateClass, String url, String apiKey, HttpJsonRequestFactory jsonRequestFactory, TConventions conventions,
    Action0 onDispose) {
    this.connectionStateClass = tConnectionStateClass;
    connectionStatusChanged = new ArrayList<>();
    connectionStatusChanged.add(new EventHandler<VoidArgs>() {
      @Override
      public void handle(Object sender, VoidArgs event) {
        logOnConnectionStatusChanged(sender, event);
      }
    });

    synchronized (RemoteDatabaseChanges.class) {
      connectionCounter++;

      id = connectionCounter + "/" + Base62Util.base62Random();
    }
    this.url = url;
    this.credentials = new OperationCredentials(apiKey);
    this.jsonRequestFactory = jsonRequestFactory;
    this.conventions = conventions;
    this.onDispose = onDispose;
    establishConnection();
  }


  public TConventions getConventions() {
    return conventions;
  }

  @Override
  public void addConnectionStatusChanged(EventHandler<VoidArgs> handler) {
    connectionStatusChanged.add(handler);
  }

  @Override
  public void removeConnectionStatusChanges(EventHandler<VoidArgs> handler) {
    connectionStatusChanged.remove(handler);
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @SuppressWarnings({"unused", "boxing"})
  protected void logOnConnectionStatusChanged(Object sender, EventArgs eventArgs) {
    logger.info("Connection (%s) status changed, new status: %s", url, connected);
  }

  @SuppressWarnings("null")
  public void establishConnection() {
    if (disposed) {
      return ;
    }

    if (clientSideHeartbeatTimer != null) {
      clientSideHeartbeatTimer.cancel();
      clientSideHeartbeatTimer = null;
    }

    CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null, url + "/changes/events?id=" + id, HttpMethods.GET, null, credentials, conventions, null, null);
    requestParams.setAvoidCachingRequest(true);
    requestParams.setDisableRequestCompression(true);
    logger.info("Trying to connect to %s with id %s", requestParams.getUrl(), id);
    boolean retry = false;
    IObservable<String> serverEvents = null;
    try {
      serverEvents = jsonRequestFactory.createHttpJsonRequest(requestParams).serverPull();
    } catch (Exception e) {
      logger.warnException("Could not connect to server: " + url + " and id  " + id, e);
      connected = false;
      EventHelper.invoke(connectionStatusChanged, this, EventArgs.EMPTY);

      if (disposed) {
        throw e;
      }
      Reference<Boolean> timeoutRef = new Reference<>();
      if (!HttpConnectionHelper.isServerDown(e, timeoutRef)) {
        throw e;
      }
      if (HttpConnectionHelper.isHttpStatus(e, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_FORBIDDEN, HttpStatus.SC_SERVICE_UNAVAILABLE)) {
        throw e;
      }
      logger.warn("Failed to connect to %s with id %s, will try again in 15 seconds", url, id);
      retry = true;
    }
    if (retry) {
      try {
        Thread.sleep(15000);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      establishConnection();
      return;
    }

    if (disposed) {
      connected = false;
      EventHelper.invoke(connectionStatusChanged, this, EventArgs.EMPTY);
      throw new IllegalStateException("RemoteDatabaseChanges was disposed!");
    }

    connected = true;
    EventHelper.invoke(connectionStatusChanged, this, EventArgs.EMPTY);
    connection = (Closeable) serverEvents;
    serverEvents.subscribe(this);

    clientSideHeartbeatTimer = new Timer("Changes Client Heartbeat", true);
    clientSideHeartbeatTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        clientSideHeartbeat();
      }
    }, 10000, 10000);

    subscribeOnServer();
  }

  protected void clientSideHeartbeat() {
    long elapsedTimeSinceHeartbeat = new Date().getTime() - lastHeartbeat.getTime();
    if (elapsedTimeSinceHeartbeat < 45 * 1000) {
      return;
    }
    onError(new TimeoutException("Over 45 seconds have passed since we got a server heartbeat, even though we should get one every 10 seconds or so.\r\n This connection is now presumed dead, and will attempt reconnection"));
  }

  protected void send(String command, String value) {
    synchronized (this) {
      logger.info("Sending command %s - %s to %s with id %s", command, value, url, id);

      String sendUrl = url + "/changes/config?id=" + id + "&command=" + command;
      if (StringUtils.isNotEmpty(value)) {
        sendUrl += "&value=" + UrlUtils.escapeUriString(value);
      }

      CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null, sendUrl, HttpMethods.GET, null, credentials, conventions, null, null);
      requestParams.setAvoidCachingRequest(true);
      try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(requestParams)) {
        request.executeRequest();
      }
    }
  }

  @Override
  public void close() {
    if (disposed) {
      return;
    }
    disposed = true;
    onDispose.apply();

    if (clientSideHeartbeatTimer != null) {
      clientSideHeartbeatTimer.cancel();
    }
    clientSideHeartbeatTimer = null;

    send("disconnect", null);

    try {
      if (connection != null) {
        connection.close();
      }
    } catch (Exception e) {
      logger.errorException("Got error from server connection for " + url + " on id " + id , e);
    }
  }

  @Override
  public void onError(Exception error) {
    logger.errorException("Got error from server connection for " + url + " on id " + id, error);
    renewConnection();
  }

  private void renewConnection() {
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      // ignore
    }
    try {
      establishConnection();
    } catch (Exception e) {
      for (Map.Entry<String, TConnectionState> keyValuePair : counters) {
        keyValuePair.getValue().error(e);
      }
      counters.clear();
    }
  }

  @Override
  public void onNext(String dataFromConnection) {
    lastHeartbeat = new Date();
    RavenJObject ravenJObject = RavenJObject.parse(dataFromConnection);
    RavenJObject value = ravenJObject.value(RavenJObject.class, "Value");
    String type = ravenJObject.value(String.class, "Type");

    logger.debug("Got notification from %s id %s of type %s", url, id, dataFromConnection);

    try {
      switch (type) {
        case "Disconnect":
          if (connection != null) {
            connection.close();
          }
          renewConnection();
          break;
        case "Initialized":
        case "Heartbeat":
          break;
        default:
          notifySubscribers(type, value, counters);
          break;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("hiding")
  protected abstract void notifySubscribers(String type, RavenJObject value, AtomicDictionary<TConnectionState> counters);

  protected abstract void subscribeOnServer();

  @Override
  public void onCompleted() {
    //empty by design
  }

  protected TConnectionState getOrAddConnectionState(final String name, final String watchCommand,
                                                          final String unwatchCommand, final Action0 afterConnection,
                                                          final Action0 beforeDisconnect, final String value) {

    TConnectionState counter = counters.getOrAdd(name, new Function1<String, TConnectionState>() {
      @Override
      public TConnectionState apply(String s) {

        Action0 onZero = new Action0() {
          @Override
          public void apply() {
            beforeDisconnect.apply();
            send(unwatchCommand, value);
            counters.remove(name);
          }
        };

        Action1<TConnectionState> ensureConnection = new Action1<TConnectionState>() {
          @Override
          public void apply(final TConnectionState existingConnectionState) {
            if (counters.get(name) != null) {
              return;
            }
            counters.getOrAdd(name, new Function1<String, TConnectionState>() {
                      @Override
                      public TConnectionState apply(String input) {
                        return existingConnectionState;
                      }
                    }
            );

            afterConnection.apply();
            send(watchCommand, value);
          }
        };

        Action0 counterSubscriptionTask = new Action0() {
          @Override
          public void apply() {
            afterConnection.apply();
            send(watchCommand, value);
          }
        };

        counterSubscriptionTask.apply();

        return createTConnectionState(onZero, ensureConnection);
      }

    });
    return counter;
  }

  private TConnectionState createTConnectionState(Object... args) {
    try {
      Constructor<TConnectionState> declaredConstructor = (Constructor<TConnectionState>) connectionStateClass.getDeclaredConstructors()[0];
      TConnectionState tConnectionState = declaredConstructor.newInstance(args);
      return tConnectionState;
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new IllegalStateException(e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

}
