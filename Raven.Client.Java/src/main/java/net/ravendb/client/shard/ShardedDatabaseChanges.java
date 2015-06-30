package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.data.*;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.changes.IObservable;


public class ShardedDatabaseChanges implements IDatabaseChanges {
  private final IDatabaseChanges[] shardedDatabaseChanges;
  private boolean connected;
  private List<EventHandler<VoidArgs>> connectionStatusChanged;

  public ShardedDatabaseChanges(IDatabaseChanges[] shardedDatabaseChanges) {
    this.shardedDatabaseChanges = shardedDatabaseChanges;
  }

  @Override
  public boolean isConnected() {
    return connected;
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
  public IObservable<IndexChangeNotification> forIndex(String indexName) {
    List<IObservable<IndexChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forIndex(indexName));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocument(String docId) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocument(docId));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forAllDocuments() {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forAllDocuments());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<IndexChangeNotification> forAllIndexes() {
    List<IObservable<IndexChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forAllIndexes());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<TransformerChangeNotification> forAllTransformers() {
    List<IObservable<TransformerChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forAllTransformers());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsStartingWith(String docIdPrefix) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocumentsStartingWith(docIdPrefix));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(String collectionName) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocumentsInCollection(collectionName));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(Class<?> clazz) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocumentsInCollection(clazz));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsOfType(String typeName) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocumentsOfType(typeName));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsOfType(Class<?> clazz) {
    List<IObservable<DocumentChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDocumentsOfType(clazz));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<ReplicationConflictNotification> forAllReplicationConflicts() {
    List<IObservable<ReplicationConflictNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forAllReplicationConflicts());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<BulkInsertChangeNotification> forBulkInsert() {
    List<IObservable<BulkInsertChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forBulkInsert());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<BulkInsertChangeNotification> forBulkInsert(UUID operationId) {
    List<IObservable<BulkInsertChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forBulkInsert(operationId));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DataSubscriptionChangeNotification> forAllDataSubscriptions() {
    List<IObservable<DataSubscriptionChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forAllDataSubscriptions());
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public IObservable<DataSubscriptionChangeNotification> forDataSubscription(long id) {
    List<IObservable<DataSubscriptionChangeNotification>> observables = new ArrayList<>(shardedDatabaseChanges.length);
    for (IDatabaseChanges changes: shardedDatabaseChanges) {
      observables.add(changes.forDataSubscription(id));
    }
    return new ShardedObservable<>(observables);
  }

  @Override
  public void waitForAllPendingSubscriptions() {
    for (IDatabaseChanges shardedDatabaseChange: shardedDatabaseChanges) {
      shardedDatabaseChange.waitForAllPendingSubscriptions();
    }
  }

}
