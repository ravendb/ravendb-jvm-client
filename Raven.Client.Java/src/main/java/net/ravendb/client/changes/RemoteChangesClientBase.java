package net.ravendb.client.changes;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventArgs;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.abstractions.util.AtomicDictionary;
import net.ravendb.abstractions.util.Base62Util;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.IReplicationInformerBase;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.Convention;
import net.ravendb.client.utils.UrlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;


public abstract class RemoteChangesClientBase<TChangesApi extends IConnectableChanges, TConnectionState extends IChangesConnectionState>
  implements CleanCloseable, IObserver<String>, IConnectableChanges {
  protected static final ILog logger = LogManager.getCurrentClassLogger();

  private Timer clientSideHeartbeatTimer;

  private final String url;
  private OperationCredentials credentials;
  private final HttpJsonRequestFactory jsonRequestFactory;
  protected final Convention conventions;
  @SuppressWarnings("rawtypes")
  private final IReplicationInformerBase replicationInformer;

  private final Action0 onDispose;

  private Closeable connection;
  private Date lastHeartbeat = new Date();

  private static int connectionCounter;
  private final String id;

  protected final AtomicDictionary<DatabaseConnectionState> counters = new AtomicDictionary<>(String.CASE_INSENSITIVE_ORDER);

  private boolean connected;

  private List<EventHandler<VoidArgs>> connectionStatusChanged;

  private volatile boolean disposed;


  @SuppressWarnings("rawtypes")
  public RemoteChangesClientBase(String url, String apiKey, HttpJsonRequestFactory jsonRequestFactory, Convention conventions,
    IReplicationInformerBase replicationInformer, Action0 onDispose) {
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
    this.replicationInformer = replicationInformer;
    this.onDispose = onDispose;
    establishConnection();
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

    CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null, url + "/changes/events?id=" + id, HttpMethods.GET, null, credentials, conventions);
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
        logger.warn("Failed to connect to %s with id %s, probably shutting down...", url, id);
        throw e;
      }
      Reference<Integer> codeRef = new Reference<>();
      if (replicationInformer.isHttpStatus(e, codeRef, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_FORBIDDEN, HttpStatus.SC_SERVICE_UNAVAILABLE, HttpStatus.SC_UNAUTHORIZED)) {
        logger.error("Failed to connect to %s with id %s, server returned with an error code: %d", url, id, codeRef.value);
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

      CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null, sendUrl, HttpMethods.GET, null, credentials, conventions);
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
      for (Map.Entry<String, DatabaseConnectionState> keyValuePair : counters) {
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
          notifySubscribers(type, value, counters.getValuesSnapshot());
          break;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("hiding")
  protected abstract void notifySubscribers(String type, RavenJObject value, List<DatabaseConnectionState> connections);

  protected abstract void subscribeOnServer();

  @Override
  public void onCompleted() {
    //empty by design
  }

}
