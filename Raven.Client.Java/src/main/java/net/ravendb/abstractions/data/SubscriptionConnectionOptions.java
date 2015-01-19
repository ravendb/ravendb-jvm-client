package net.ravendb.abstractions.data;

import java.util.concurrent.atomic.AtomicInteger;

import net.ravendb.abstractions.util.Base62Util;


public class SubscriptionConnectionOptions {

  private static AtomicInteger connectionCounter = new AtomicInteger();

  public SubscriptionConnectionOptions() {
    connectionId = connectionCounter.incrementAndGet() + "/" + Base62Util.base62Random();
    batchOptions = new SubscriptionBatchOptions();
    clientAliveNotificationInterval = 2 * 60 * 1000L;
  }

  public String connectionId;

  private SubscriptionBatchOptions batchOptions;

  private Long clientAliveNotificationInterval;

  private boolean ignoreSubscribersErrors;

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

  public Long getClientAliveNotificationInterval() {
    return clientAliveNotificationInterval;
  }

  public void setClientAliveNotificationInterval(Long clientAliveNotificationInterval) {
    this.clientAliveNotificationInterval = clientAliveNotificationInterval;
  }

  public boolean isIgnoreSubscribersErrors() {
    return ignoreSubscribersErrors;
  }

  public void setIgnoreSubscribersErrors(boolean ignoreSubscribersErrors) {
    this.ignoreSubscribersErrors = ignoreSubscribersErrors;
  }

}
