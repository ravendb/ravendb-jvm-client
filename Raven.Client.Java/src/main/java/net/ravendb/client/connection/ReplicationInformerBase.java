package net.ravendb.client.connection;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.exceptions.HttpOperationException;
import net.ravendb.abstractions.exceptions.ServerClientException;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.client.connection.ReplicationInformer.FailoverStatusChangedEventArgs;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.Convention;
import net.ravendb.client.document.FailoverBehavior;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import com.google.common.base.Throwables;

public abstract class ReplicationInformerBase<T> implements IReplicationInformerBase<T> {
  protected static ILog log = LogManager.getCurrentClassLogger();

  protected boolean firstTime = true;
  protected Convention conventions;
  private final HttpJsonRequestFactory requestFactory;
  protected Date lastReplicationUpdate = new Date(0);
  protected final Object replicationLock = new Object();
  private static List<OperationMetadata> EMPTY = new ArrayList<>();
  protected static AtomicInteger readStripingBase = new AtomicInteger(0);
  private int delayTimeInMiliSec;

  protected List<OperationMetadata> replicationDestinations = new ArrayList<>();

  protected final Map<String, FailureCounter> failureCounts = new ConcurrentHashMap<>();

  protected Thread refreshReplicationInformationTask;


  protected List<EventHandler<FailoverStatusChangedEventArgs>> failoverStatusChanged = new ArrayList<>();

  @Override
  public abstract void clearReplicationInformationLocalCache(T client);

  @Override
  public int getDelayTimeInMiliSec() {
    return delayTimeInMiliSec;
  }

  @Override
  public void setDelayTimeInMiliSec(int delayTimeInMiliSec) {
    this.delayTimeInMiliSec = delayTimeInMiliSec;
  }

  @Override
  public void addFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
    failoverStatusChanged.add(event);
  }

  @Override
  public void removeFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
    failoverStatusChanged.remove(event);
  }

  @Override
  public List<OperationMetadata> getReplicationDestinations() {
    return this.replicationDestinations;
  }

  @Override
  public List<OperationMetadata> getReplicationDestinationsUrls() {
    if (FailoverBehavior.FAIL_IMMEDIATELY.equals(this.conventions.getFailoverBehavior())) {
      return EMPTY;
    }
    List<OperationMetadata> result = new ArrayList<>();
    for (OperationMetadata opMeta : this.replicationDestinations) {
      result.add(new OperationMetadata(opMeta));
    }
    return result;
  }

  protected ReplicationInformerBase(Convention conventions, HttpJsonRequestFactory requestFactory, int delayTime) {
    this.conventions = conventions;
    this.requestFactory = requestFactory;
    this.replicationDestinations = new ArrayList<>();
    this.delayTimeInMiliSec = delayTime;
  }

  public static class FailureCounter {

    private AtomicLong value = new AtomicLong();
    private Date lastCheck;
    private boolean forceCheck;

    private AtomicReference<Thread> checkDestination = new AtomicReference<>();


    public AtomicReference<Thread> getCheckDestination() {
      return checkDestination;
    }

    public void setCheckDestination(AtomicReference<Thread> checkDestination) {
      this.checkDestination = checkDestination;
    }

    public AtomicLong getValue() {
      return value;
    }

    public void setValue(AtomicLong value) {
      this.value = value;
    }

    public Date getLastCheck() {
      return lastCheck;
    }

    public void setLastCheck(Date lastCheck) {
      this.lastCheck = lastCheck;
    }

    public boolean isForceCheck() {
      return forceCheck;
    }

    public void setForceCheck(boolean forceCheck) {
      this.forceCheck = forceCheck;
    }

    public FailureCounter() {
      this.lastCheck = new Date();
    }

    public long increment() {
      this.forceCheck = false;
      this.lastCheck = new Date();
      return value.incrementAndGet();
    }

    public long reset() {
      long oldVal = this.value.get();
      value.compareAndSet(oldVal, 0);
      lastCheck = new Date();
      forceCheck = false;
      return oldVal;
    }

  }

  @Override
  public AtomicLong getFailureCount(String operationUrl) {
    return getHolder(operationUrl).getValue();
  }

  @Override
  public Date getFailureLastCheck(String operationUrl) {
    return getHolder(operationUrl).getLastCheck();
  }

  public boolean shouldExecuteUsing(final OperationMetadata operationMetadata, final OperationMetadata primaryOperation, int currentRequest, HttpMethods method, boolean primary, Exception error) {
    if (primary == false) {
      assertValidOperation(method, error);
    }

    FailureCounter failureCounter = getHolder(operationMetadata.getUrl());
    if (failureCounter.getValue().longValue() == 0) {
      return true;
    }

    if (failureCounter.isForceCheck()) {
      return true;
    }

    Thread currentTask = failureCounter.getCheckDestination().get();
    if ((currentTask == null  || !currentTask.isAlive()) && delayTimeInMiliSec > 0) {
      Thread checkDestination = new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < 3; i++) {
              OperationResult<Object> r = tryOperation(new Function1<OperationMetadata, Object>() {
                @Override
                public OperationResult<Object> apply(OperationMetadata metadata) {
                    CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null, getServerCheckUrl(metadata.getUrl()), HttpMethods.GET, new RavenJObject(), metadata.getCredentials(),  conventions);
                    requestFactory.createHttpJsonRequest(requestParams).readResponseJson();
                    return null;
                }
              }, operationMetadata, primaryOperation, true);
              if (r.isSuccess()) {
                return;
              }

              try {
                Thread.sleep(delayTimeInMiliSec);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
          }
        };
      });

      if (failureCounter.getCheckDestination().compareAndSet(currentTask, checkDestination)) {
        checkDestination.start();
      }
    }

    return false;
  }

  protected abstract String getServerCheckUrl(String baseUrl);

  protected void assertValidOperation(HttpMethods method, Exception error) {
    if (conventions.getFailoverBehaviorWithoutFlags().contains(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES)) {
      if (HttpMethods.GET.equals(method)) {
        return;
      }
    }
    if (conventions.getFailoverBehaviorWithoutFlags().contains(
      FailoverBehavior.ALLOW_READS_FROM_SECONDARIES_AND_WRITES_TO_SECONDARIES)) {
      return;
    }
    if (conventions.getFailoverBehaviorWithoutFlags().contains(FailoverBehavior.FAIL_IMMEDIATELY)) {
      if (conventions.getFailoverBehaviorWithoutFlags().contains(FailoverBehavior.READ_FROM_ALL_SERVERS)) {
        if (HttpMethods.GET.equals(method)) {
          return;
        }
      }
    }
    throw new IllegalStateException("Could not replicate " + method
      + " operation to secondary node, failover behavior is: " + conventions.getFailoverBehavior(), error);
  }

  protected FailureCounter getHolder(String operationUrl) {
    if (!failureCounts.containsKey(operationUrl)) {
      failureCounts.put(operationUrl, new FailureCounter());
    }
    return failureCounts.get(operationUrl);

  }


  public boolean isFirstFailure(String operationUrl) {
    FailureCounter value = getHolder(operationUrl);
    return value.getValue().longValue() == 0;
  }

  public void incrementFailureCount(String operationUrl) {
    FailureCounter value = getHolder(operationUrl);
    value.setForceCheck(false);
    long current = value.getValue().incrementAndGet();
    if (current == 1) { // first failure
      EventHelper.invoke(failoverStatusChanged, this, new FailoverStatusChangedEventArgs(operationUrl, true));
    }
  }

  protected static boolean isInvalidDestinationsDocument(JsonDocument document) {
    return document == null || document.getDataAsJson().containsKey("Destinations") == false
      || document.getDataAsJson().get("Destinations") == null
      || JTokenType.NULL.equals(document.getDataAsJson().get("Destinations").getType());
  }

  public void resetFailureCount(String operationUrl) {
    FailureCounter value = getHolder(operationUrl);
    long oldVal = value.getValue().getAndSet(0);
    value.setLastCheck(new Date());
    value.setForceCheck(false);
    if (oldVal != 0) {
      EventHelper.invoke(failoverStatusChanged, this, new FailoverStatusChangedEventArgs(operationUrl, false));
    }
  }

  @Override
  public int getReadStripingBase(boolean increment) {
    return increment ? readStripingBase.incrementAndGet() : readStripingBase.get();
  }


  @Override
  public <S> S executeWithReplication(HttpMethods method, String primaryUrl, OperationCredentials primaryCredentials, int currentRequest,
    int currentReadStripingBase, Function1<OperationMetadata, S> operation) throws ServerClientException {

    List<OperationMetadata> localReplicationDestinations = getReplicationDestinationsUrls(); // thread safe copy
    OperationMetadata primaryOperation = new OperationMetadata(primaryUrl, primaryCredentials);

    boolean shouldReadFromAllServers = conventions.getFailoverBehavior().contains(
      FailoverBehavior.READ_FROM_ALL_SERVERS);

    OperationResult<S> operationResult = new OperationResult<>();

    if (shouldReadFromAllServers && HttpMethods.GET.equals(method)) {
      int replicationIndex = currentReadStripingBase % (localReplicationDestinations.size() + 1);
      // if replicationIndex == destinations count, then we want to use the master
      // if replicationIndex < 0, then we were explicitly instructed to use the master
      if (replicationIndex < localReplicationDestinations.size() && replicationIndex >= 0) {
        // if it is failing, ignore that, and move to the master or any of the replicas
        if (shouldExecuteUsing(localReplicationDestinations.get(replicationIndex), primaryOperation, currentRequest, method, false, null)) {

           operationResult = tryOperation(operation, localReplicationDestinations.get(replicationIndex), primaryOperation, true);
           if (operationResult.success) {
             return operationResult.result;
           }
        }
      }
    }

    if (shouldExecuteUsing(primaryOperation, primaryOperation, currentRequest, method, true, null)) {
      operationResult = tryOperation(operation, primaryOperation, null, !operationResult.wasTimeout && localReplicationDestinations.size() > 0);
      if (operationResult.isSuccess()) {
        return operationResult.result;
      }
      incrementFailureCount(primaryOperation.getUrl());
      if (!operationResult.wasTimeout && isFirstFailure(primaryOperation.getUrl())) {

        operationResult = tryOperation(operation, primaryOperation, null, localReplicationDestinations.size() > 0);
        if (operationResult.isSuccess()) {
          return operationResult.result;
        }
        incrementFailureCount(primaryOperation.getUrl());
      }

    }

    for (int i = 0; i < localReplicationDestinations.size(); i++) {
      OperationMetadata replicationDestination = localReplicationDestinations.get(i);
      if (!shouldExecuteUsing(replicationDestination, primaryOperation, currentRequest, method, false, operationResult.getError())) {
        continue;
      }
      boolean hasMoreReplicationDestinations = localReplicationDestinations.size() > i + 1;

      operationResult = tryOperation(operation, replicationDestination, primaryOperation, !operationResult.wasTimeout && hasMoreReplicationDestinations);
      if (operationResult.isSuccess()) {
        return operationResult.result;
      }
      incrementFailureCount(replicationDestination.getUrl());
      if (!operationResult.wasTimeout && isFirstFailure(replicationDestination.getUrl())) {
        operationResult =  tryOperation(operation, replicationDestination, primaryOperation, hasMoreReplicationDestinations);
        if (operationResult.success) {
          return operationResult.result;
        }
        incrementFailureCount(replicationDestination.getUrl());
      }
    }
    // this should not be thrown, but since I know the value of should...
    throw new IllegalStateException("Attempted to connect to master and all replicas have failed, giving up. There is a high probability of a network problem preventing access to all the replicas. Failed to get in touch with any of the " + (1 + localReplicationDestinations.size()) + " Raven instances.");
  }


  protected <S> OperationResult<S> tryOperation(Function1<OperationMetadata, S> operation, OperationMetadata operationMetadata, OperationMetadata primaryOperationMetadata, boolean avoidThrowing) {
    boolean tryWithPrimaryCredentials = isFirstFailure(operationMetadata.getUrl()) && primaryOperationMetadata != null;
    boolean shouldTryAgain = false;
    try {

      S result = operation.apply(tryWithPrimaryCredentials ? new OperationMetadata(operationMetadata.getUrl(), primaryOperationMetadata.getCredentials()) : operationMetadata);
      resetFailureCount(operationMetadata.getUrl());
      return new OperationResult<>(result, true);
    } catch (Exception e) {
      if (tryWithPrimaryCredentials && operationMetadata.getCredentials().getApiKey() != null) {
        incrementFailureCount(operationMetadata.getUrl());

        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof HttpOperationException) {
          HttpOperationException webException = (HttpOperationException) rootCause;
          if (webException.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            shouldTryAgain = true;
          }
        }
      }

      if (shouldTryAgain == false) {
        if (avoidThrowing == false) {
          throw e;
        }

        Reference<Boolean> wasTimeout = new Reference<>();
        if (isServerDown(e, wasTimeout)) {
          return new OperationResult<>(null, wasTimeout.value, false, e);
        }
        throw e;
      }
    }

    return tryOperation(operation, operationMetadata, primaryOperationMetadata, avoidThrowing);
  }

  @Override
  public boolean isHttpStatus(Exception e, int... httpStatusCode) {
    if (e instanceof HttpOperationException) {
      HttpOperationException hoe = (HttpOperationException) e;
      if (ArrayUtils.contains(httpStatusCode, hoe.getStatusCode())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isServerDown(Exception e, Reference<Boolean> timeout) {
    timeout.value = Boolean.FALSE;
    Throwable rootCause = Throwables.getRootCause(e);
    if (rootCause instanceof SocketTimeoutException) {
      timeout.value = Boolean.TRUE;
      return true;
    }
    if (rootCause instanceof SocketException) {
      return true;
    }
    return false;
  }


  public void dispose() throws InterruptedException {
    Thread replicationInformationTaskCopy = refreshReplicationInformationTask;
    if (replicationInformationTaskCopy != null) {
      replicationInformationTaskCopy.join();
    }
  }

  @Override
  public void forceCheck(String primaryUrl, boolean shouldForceCheck) {
    FailureCounter failureCounter = getHolder(primaryUrl);
    failureCounter.setForceCheck(shouldForceCheck);
  }

  public static class OperationResult<T> {
    private T result;
    private boolean wasTimeout;
    private boolean success;
    private Exception error;


    public Exception getError() {
      return error;
    }


    public void setError(Exception error) {
      this.error = error;
    }

    public T getResult() {
      return result;
    }

    public void setResult(T result) {
      this.result = result;
    }

    public boolean isWasTimeout() {
      return wasTimeout;
    }

    public void setWasTimeout(boolean wasTimeout) {
      this.wasTimeout = wasTimeout;
    }

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public OperationResult(T result, boolean wasTimeout, boolean success, Exception error) {
      super();
      this.result = result;
      this.wasTimeout = wasTimeout;
      this.success = success;
      this.error = error;
    }

    public OperationResult(T result, boolean success) {
      super();
      this.result = result;
      this.success = success;
    }

    public OperationResult() {
      super();
    }

  }

}
