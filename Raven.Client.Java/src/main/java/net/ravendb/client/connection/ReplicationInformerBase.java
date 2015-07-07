package net.ravendb.client.connection;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;
import net.ravendb.client.QueryConvention;
import net.ravendb.client.connection.ReplicationInformer.FailoverStatusChangedEventArgs;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.connection.request.FailureCounters;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.metrics.RequestTimeMetric;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReplicationInformerBase<T> implements IReplicationInformerBase<T> {

  protected static ILog log = LogManager.getCurrentClassLogger();

  protected QueryConvention conventions;
  private final HttpJsonRequestFactory requestFactory;
  private static List<OperationMetadata> EMPTY = new ArrayList<>();
  protected static AtomicInteger readStripingBase = new AtomicInteger(0);
  private int delayTimeInMiliSec;
  protected List<OperationMetadata> replicationDestinations = new ArrayList<>();
  private FailureCounters failureCounters;

  @Override
  public void addFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
     failureCounters.addFailoverStatusChanged(event);
  }

  @Override
  public void removeFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
    failureCounters.removeFailoverStatusChanged(event);
  }

  @Override
  public int getDelayTimeInMiliSec() {
    return delayTimeInMiliSec;
  }

  @Override
  public void setDelayTimeInMiliSec(int delayTimeInMiliSec) {
    this.delayTimeInMiliSec = delayTimeInMiliSec;
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

  protected ReplicationInformerBase(QueryConvention conventions, HttpJsonRequestFactory requestFactory, int delayTime) {
    this.conventions = conventions;
    this.requestFactory = requestFactory;
    this.replicationDestinations = new ArrayList<>();
    this.delayTimeInMiliSec = delayTime;
    this.failureCounters = new FailureCounters();
  }

  @Override
  public FailureCounters getFailureCounters() {
    return failureCounters;
  }

  public abstract void refreshReplicationInformation(T client);

  @Override
  public abstract void clearReplicationInformationLocalCache(T client);

  public abstract void updateReplicationInformationFromDocument(JsonDocument document);

  @SuppressWarnings("unused")
  public boolean shouldExecuteUsing(final OperationMetadata operationMetadata,
                                    final OperationMetadata primaryOperation, int currentRequest, HttpMethods method, boolean primary, Exception error) {
    if (primary == false) {
      assertValidOperation(method, error);
    }

    FailureCounters.FailureCounter failureCounter = failureCounters.getHolder(operationMetadata.getUrl());
    if (failureCounter.getValue().longValue() == 0) {
      return true;
    }

    if (failureCounter.isForceCheck()) {
      return true;
    }

    Thread currentTask = failureCounter.getCheckDestination().get();
    if ((currentTask == null || !currentTask.isAlive()) && delayTimeInMiliSec > 0) {
      Thread checkDestination = new Thread(new Runnable() {

        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
          for (int i = 0; i < 3; i++) {
            OperationResult<Object> r = tryOperation(new Function1<OperationMetadata, Object>() {

              @Override
              public OperationResult<Object> apply(OperationMetadata metadata) {
                CreateHttpJsonRequestParams requestParams = new CreateHttpJsonRequestParams(null,
                        getServerCheckUrl(metadata.getUrl()), HttpMethods.GET, metadata.getCredentials(),
                        conventions, null, null);
                try (HttpJsonRequest request = requestFactory.createHttpJsonRequest(requestParams)) {
                  request.readResponseJson();
                }
                return null;
              }
            }, operationMetadata, primaryOperation, true);
            if (r.isSuccess()) {
              failureCounters.resetFailureCount(operationMetadata.getUrl());
              return;
            }

            try {
              Thread.sleep(delayTimeInMiliSec);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      });

      if (failureCounter.getCheckDestination().compareAndSet(currentTask, checkDestination)) {
        checkDestination.start();
      }
    }

    return false;
  }

  protected abstract String getServerCheckUrl(String baseUrl);

  protected void assertValidOperation(HttpMethods method, Exception error) {
    if (conventions.getFailoverBehaviorWithoutFlags().contains(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES)
            || conventions.getFailoverBehaviorWithoutFlags().contains(FailoverBehavior.ALLOW_READ_FROM_SECONDARIES_WHEN_REQUEST_TIME_THRESHOLD_IS_SURPASSED)) {
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

  protected static boolean isInvalidDestinationsDocument(JsonDocument document) {
    return document == null || document.getDataAsJson().containsKey("Destinations") == false
            || document.getDataAsJson().get("Destinations") == null
            || JTokenType.NULL.equals(document.getDataAsJson().get("Destinations").getType());
  }

  @Override
  public int getReadStripingBase(boolean increment) {
    return increment ? readStripingBase.incrementAndGet() : readStripingBase.get();
  }

  @SuppressWarnings("synthetic-access")
  @Override
  public <S> S executeWithReplication(HttpMethods method, String primaryUrl, OperationCredentials primaryCredentials,
                                      RequestTimeMetric primaryRequestTimeMetric,
                                      int currentRequest, int currentReadStripingBase, Function1<OperationMetadata, S> operation) {

    List<OperationMetadata> localReplicationDestinations = getReplicationDestinationsUrls(); // thread safe copy
    OperationMetadata primaryOperation = new OperationMetadata(primaryUrl, primaryCredentials, null);

    OperationResult<S> operationResult = new OperationResult<>();

    boolean shouldReadFromAllServers = conventions.getFailoverBehavior().contains(
            FailoverBehavior.READ_FROM_ALL_SERVERS);

    boolean allowReadFromSecondariesWhenRequestTimeThresholdIsPassed = conventions.getFailoverBehavior().contains(FailoverBehavior.ALLOW_READ_FROM_SECONDARIES_WHEN_REQUEST_TIME_THRESHOLD_IS_SURPASSED);

    if (HttpMethods.GET.equals(method) && (shouldReadFromAllServers || allowReadFromSecondariesWhenRequestTimeThresholdIsPassed)) {
      int replicationIndex = -1;
      if (allowReadFromSecondariesWhenRequestTimeThresholdIsPassed && primaryRequestTimeMetric != null && primaryRequestTimeMetric.rateSurpassed(conventions)) {
        replicationIndex = currentReadStripingBase % localReplicationDestinations.size();
      } else if (shouldReadFromAllServers) {
        replicationIndex = currentReadStripingBase % (localReplicationDestinations.size() + 1);
      }

      // if replicationIndex == destinations count, then we want to use the
      // master
      // if replicationIndex < 0, then we were explicitly instructed to use the
      // master
      if (replicationIndex < localReplicationDestinations.size() && replicationIndex >= 0) {
        // if it is failing, ignore that, and move to the master or any of the
        // replicas
        if (shouldExecuteUsing(localReplicationDestinations.get(replicationIndex), primaryOperation, currentRequest,
                method, false, null)) {

          operationResult = tryOperation(operation, localReplicationDestinations.get(replicationIndex),
                  primaryOperation, true);
          if (operationResult.success) {
            return operationResult.result;
          }
        }
      }
    }

    if (shouldExecuteUsing(primaryOperation, primaryOperation, currentRequest, method, true, null)) {
      operationResult = tryOperation(operation, primaryOperation, null, !operationResult.wasTimeout
              && localReplicationDestinations.size() > 0);
      if (operationResult.isSuccess()) {
        return operationResult.result;
      }
      failureCounters.incrementFailureCount(primaryOperation.getUrl());
      if (!operationResult.wasTimeout && failureCounters.isFirstFailure(primaryOperation.getUrl())) {

        operationResult = tryOperation(operation, primaryOperation, null, localReplicationDestinations.size() > 0);
        if (operationResult.isSuccess()) {
          return operationResult.result;
        }
        failureCounters.incrementFailureCount(primaryOperation.getUrl());
      }
    }

    for (int i = 0; i < localReplicationDestinations.size(); i++) {
      OperationMetadata replicationDestination = localReplicationDestinations.get(i);
      if (!shouldExecuteUsing(replicationDestination, primaryOperation, currentRequest, method, false,
              operationResult.getError())) {
        continue;
      }
      boolean hasMoreReplicationDestinations = localReplicationDestinations.size() > i + 1;

      operationResult = tryOperation(operation, replicationDestination, primaryOperation, !operationResult.wasTimeout
              && hasMoreReplicationDestinations);
      if (operationResult.isSuccess()) {
        return operationResult.result;
      }
      failureCounters.incrementFailureCount(replicationDestination.getUrl());
      if (!operationResult.wasTimeout && failureCounters.isFirstFailure(replicationDestination.getUrl())) {
        operationResult = tryOperation(operation, replicationDestination, primaryOperation,
                hasMoreReplicationDestinations);
        if (operationResult.success) {
          return operationResult.result;
        }
        failureCounters.incrementFailureCount(replicationDestination.getUrl());
      }
    }
    // this should not be thrown, but since I know the value of should...
    throw new IllegalStateException(
            "Attempted to connect to master and all replicas have failed, giving up. There is a high probability of a network problem preventing access to all the replicas. Failed to get in touch with any of the "
                    + (1 + localReplicationDestinations.size()) + " Raven instances.");
  }

  @SuppressWarnings("boxing")
  protected <S> OperationResult<S> tryOperation(Function1<OperationMetadata, S> operation,
    OperationMetadata operationMetadata, OperationMetadata primaryOperationMetadata, boolean avoidThrowing) {
    boolean tryWithPrimaryCredentials = failureCounters.isFirstFailure(operationMetadata.getUrl()) && primaryOperationMetadata != null;
    boolean shouldTryAgain = false;
    try {
      S result = operation.apply(tryWithPrimaryCredentials ? new OperationMetadata(operationMetadata.getUrl(),
        primaryOperationMetadata.getCredentials(), primaryOperationMetadata.getClusterInformation()) : operationMetadata);
      failureCounters.resetFailureCount(operationMetadata.getUrl());
      return new OperationResult<>(result, true);
    } catch (Exception e) {
      if (tryWithPrimaryCredentials && operationMetadata.getCredentials().getApiKey() != null) {
        failureCounters.incrementFailureCount(operationMetadata.getUrl());

        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof ErrorResponseException) {
          ErrorResponseException webException = (ErrorResponseException) rootCause;
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
        if (HttpConnectionHelper.isServerDown(e, wasTimeout)) {
          return new OperationResult<>(null, wasTimeout.value, false, e);
        }
        throw e;
      }
    }

    return tryOperation(operation, operationMetadata, primaryOperationMetadata, avoidThrowing);
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
