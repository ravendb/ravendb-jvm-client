package net.ravendb.abstractions.data;

import java.util.concurrent.atomic.AtomicInteger;

import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.util.Base62Util;
import net.ravendb.client.utils.TimeSpan;


public class SubscriptionConnectionOptions {

  private static AtomicInteger connectionCounter = new AtomicInteger();

  @SuppressWarnings("boxing")
  public SubscriptionConnectionOptions() {
    connectionId = connectionCounter.incrementAndGet() + "/" + Base62Util.base62Random();
    batchOptions = new SubscriptionBatchOptions();
    clientAliveNotificationInterval = 2 * 60 * 1000;
    timeToWaitBeforeConnectionRetry = 15 * 1000;
    strategy = SubscriptionOpeningStrategy.OPEN_IF_FREE;
  }

  public SubscriptionConnectionOptions(SubscriptionBatchOptions batchOptions) {
    this();
    this.batchOptions = batchOptions;
  }

  public String connectionId;

  private SubscriptionBatchOptions batchOptions;

  private Integer timeToWaitBeforeConnectionRetry;

  private Integer clientAliveNotificationInterval;

  private boolean ignoreSubscribersErrors;

  private SubscriptionOpeningStrategy strategy;

  public Integer getTimeToWaitBeforeConnectionRetry() {
    return timeToWaitBeforeConnectionRetry;
  }

  public void setTimeToWaitBeforeConnectionRetry(Integer timeToWaitBeforeConnectionRetry) {
    this.timeToWaitBeforeConnectionRetry = timeToWaitBeforeConnectionRetry;
  }

  public String getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }

  public SubscriptionBatchOptions getBatchOptions() {
    return batchOptions;
  }

  public void setBatchOptions(SubscriptionBatchOptions batchOptions) {
    this.batchOptions = batchOptions;
  }

  public SubscriptionOpeningStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(SubscriptionOpeningStrategy strategy) {
    this.strategy = strategy;
  }

  public Integer getClientAliveNotificationInterval() {
    return clientAliveNotificationInterval;
  }

  public void setClientAliveNotificationInterval(Integer clientAliveNotificationInterval) {
    this.clientAliveNotificationInterval = clientAliveNotificationInterval;
  }

  public boolean isIgnoreSubscribersErrors() {
    return ignoreSubscribersErrors;
  }

  public void setIgnoreSubscribersErrors(boolean ignoreSubscribersErrors) {
    this.ignoreSubscribersErrors = ignoreSubscribersErrors;
  }

  @SuppressWarnings("boxing")
  public RavenJObject toRavenObject() {
    RavenJObject result = new RavenJObject();
    result.add("ConnectionId", connectionId);
    result.add("IgnoreSubscribersErrors", ignoreSubscribersErrors);
    result.add("BatchOptions", batchOptions.toRavenObject());
    result.add("ClientAliveNotificationInterval", TimeSpan.formatString(clientAliveNotificationInterval));
    return result;
  }

}
