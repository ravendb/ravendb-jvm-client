package net.ravendb.client.changes;

import java.util.UUID;

import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeNotification;
import net.ravendb.abstractions.data.IndexChangeNotification;
import net.ravendb.abstractions.data.ReplicationConflictNotification;
import net.ravendb.abstractions.data.TransformerChangeNotification;


public interface IDatabaseChanges extends IConnectableChanges {

  /**
   * Subscribe to changes for specified index only.
   * @param indexName
   */
  public IObservable<IndexChangeNotification> forIndex(String indexName);

  /**
   * Subscribe to changes for specified document only.
   * @param docId
   */
  public IObservable<DocumentChangeNotification> forDocument(String docId);

  /**
   * Subscribe to changes for all documents.
   */
  public IObservable<DocumentChangeNotification> forAllDocuments();

  /**
   * Subscribe to changes for all indexes.
   */
  public IObservable<IndexChangeNotification> forAllIndexes();

  /**
   * Subscribe to changes for all transformers.
   */
  public IObservable<TransformerChangeNotification> forAllTransformers();

  /**
   * Subscribe to changes for all documents that Id starts with given prefix.
   * @param docIdPrefix
   */
  public IObservable<DocumentChangeNotification> forDocumentsStartingWith(String docIdPrefix);

  /**
   * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
   * @param collectionName
   */
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(String collectionName);

  /**
   * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
   * @param clazz
   */
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(Class<?> clazz);

  /**
   * Subscribe to changes for all documents that belong to specified type (Raven-Clr-Type).
   * @param typeName
   */
  public IObservable<DocumentChangeNotification> forDocumentsOfType(String typeName);

  /**
   * Subscribe to changes for all documents that belong to specified type (Raven-Clr-Type).
   * @param clazz
   */
  public IObservable<DocumentChangeNotification> forDocumentsOfType(Class<?> clazz);

  /**
   * Subscribe to all replication conflicts.
   */
  public IObservable<ReplicationConflictNotification> forAllReplicationConflicts();

  /**
   * Subscribe to all bulk insert operation changes that belong to a operation with given Id.
   * @param operationId
   */
  public IObservable<BulkInsertChangeNotification> forBulkInsert(UUID operationId);

  public IObservable<BulkInsertChangeNotification> forBulkInsert();

}
