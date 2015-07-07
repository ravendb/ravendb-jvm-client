package net.ravendb.client.connection;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.client.connection.ReplicationInformer.FailoverStatusChangedEventArgs;
import net.ravendb.client.connection.request.FailureCounters;
import net.ravendb.client.metrics.RequestTimeMetric;

import java.util.List;


public interface IReplicationInformerBase<T> extends CleanCloseable {
  void addFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event);

  void removeFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event);

  int getDelayTimeInMiliSec();

  void setDelayTimeInMiliSec(int value);

  List<OperationMetadata> getReplicationDestinations();

  List<OperationMetadata> getReplicationDestinationsUrls();

  int getReadStripingBase(boolean increment);

  FailureCounters getFailureCounters();

  /**
   * Refreshes the replication information.
   * @param client
   */
  void refreshReplicationInformation(T client);

  /**
   * Clears the replication information local cache.
   * Expert use only.
   * @param client
   */
  void clearReplicationInformationLocalCache(T client);


  <S> S executeWithReplication(HttpMethods method, String primaryUrl, OperationCredentials primaryCredentials,
                                      RequestTimeMetric primaryRequestTimeMetric, int currentRequest,
    int currentReadStripingBase, Function1<OperationMetadata, S> operation);



}
