package net.ravendb.client.changes;

import java.util.UUID;

import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeNotification;
import net.ravendb.abstractions.data.IndexChangeNotification;
import net.ravendb.abstractions.data.ReplicationConflictNotification;
import net.ravendb.abstractions.data.TransformerChangeNotification;


public interface IDatabaseChanges extends IConnectableChanges {

  public IObservable<IndexChangeNotification> forIndex(String indexName);

  public IObservable<DocumentChangeNotification> forDocument(String docId);

  public IObservable<DocumentChangeNotification> forAllDocuments();

  public IObservable<IndexChangeNotification> forAllIndexes();

  public IObservable<TransformerChangeNotification> forAllTransformers();

  public IObservable<DocumentChangeNotification> forDocumentsStartingWith(String docIdPrefix);

  public IObservable<DocumentChangeNotification> forDocumentsInCollection(String collectionName);

  public IObservable<DocumentChangeNotification> forDocumentsInCollection(Class<?> clazz);

  public IObservable<DocumentChangeNotification> forDocumentsOfType(String typeName);

  public IObservable<DocumentChangeNotification> forDocumentsOfType(Class<?> clazz);

  public IObservable<ReplicationConflictNotification> forAllReplicationConflicts();

  public IObservable<BulkInsertChangeNotification> forBulkInsert(UUID operationId);

}
