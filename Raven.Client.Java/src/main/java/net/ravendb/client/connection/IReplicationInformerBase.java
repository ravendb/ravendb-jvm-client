package net.ravendb.client.connection;

import java.io.Closeable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.client.connection.ReplicationInformer.FailoverStatusChangedEventArgs;


public interface IReplicationInformerBase<T> extends CleanCloseable {
  public void addFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event);

  public void removeFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event);

  public int getDelayTimeInMiliSec();

  public void setDelayTimeInMiliSec(int value);

  public List<OperationMetadata> getReplicationDestinations();

  public List<OperationMetadata> getReplicationDestinationsUrls();

  /**
   * Refreshes the replication information.
   * @param client
   */
  public void refreshReplicationInformation(T client);

  /**
   * Clears the replication information local cache.
   * Expert use only.
   * @param client
   */
  public void clearReplicationInformationLocalCache(T client);

  /**
   * Get the current failure count for the url
   * @param operationUrl
   */
  public AtomicLong getFailureCount(String operationUrl);

  /**
   * Get failure last check time for the url
   * @param operationUrl
   */
  public Date getFailureLastCheck(String operationUrl);

  public int getReadStripingBase(boolean increment);

  public <S> S executeWithReplication(HttpMethods method, String primaryUrl, OperationCredentials primaryCredentials, int currentRequest,
    int currentReadStripingBase, Function1<OperationMetadata, S> operation);

  public void forceCheck(String primaryUrl, boolean shouldForceCheck);

  public boolean isServerDown(Exception e, Reference<Boolean> timeout);

  public boolean isHttpStatus(Exception e, int... httpStatusCode);
}
