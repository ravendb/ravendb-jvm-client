package net.ravendb.client.changes;

import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.*;

import java.util.ArrayList;
import java.util.List;


public class DatabaseConnectionState extends ConnectionStateBase {
  private Action1<DatabaseConnectionState> ensureConnection;

  private List<Action1<DocumentChangeNotification>> onDocumentChangeNotification = new ArrayList<>();
  private List<Action1<BulkInsertChangeNotification>> onBulkInsertChangeNotification = new ArrayList<>();
  private List<Action1<IndexChangeNotification>> onIndexChangeNotification = new ArrayList<>();
  private List<Action1<TransformerChangeNotification>> onTransformerChangeNotification = new ArrayList<>();
  private List<Action1<ReplicationConflictNotification>> onReplicationConflictNotification = new ArrayList<>();
  private List<Action1<DataSubscriptionChangeNotification>> onDataSubscriptionNotification = new ArrayList<>();

  public DatabaseConnectionState(Action0 onZero, Action1<DatabaseConnectionState> ensureConnection) {
    super(onZero);
    this.ensureConnection = ensureConnection;
  }

  @Override
  protected void ensureConnection() {
    ensureConnection.apply(this);
  }

  public List<Action1<DocumentChangeNotification>> getOnDocumentChangeNotification() {
    return onDocumentChangeNotification;
  }

  public List<Action1<BulkInsertChangeNotification>> getOnBulkInsertChangeNotification() {
    return onBulkInsertChangeNotification;
  }

  public List<Action1<IndexChangeNotification>> getOnIndexChangeNotification() {
    return onIndexChangeNotification;
  }

  public List<Action1<TransformerChangeNotification>> getOnTransformerChangeNotification() {
    return onTransformerChangeNotification;
  }

  public List<Action1<ReplicationConflictNotification>> getOnReplicationConflictNotification() {
    return onReplicationConflictNotification;
  }

  public List<Action1<DataSubscriptionChangeNotification>> getOnDataSubscriptionNotification() {
    return onDataSubscriptionNotification;
  }

  public void send(DocumentChangeNotification documentChangeNotification) {
    EventHelper.invoke(onDocumentChangeNotification, documentChangeNotification);
  }

  public void send(IndexChangeNotification indexChangeNotification) {
    EventHelper.invoke(onIndexChangeNotification, indexChangeNotification);
  }

  public void send(TransformerChangeNotification transformerChangeNotification) {
    EventHelper.invoke(onTransformerChangeNotification, transformerChangeNotification);
  }

  public void send(ReplicationConflictNotification replicationConflictNotification) {
    EventHelper.invoke(onReplicationConflictNotification, replicationConflictNotification);
  }

  public void send(BulkInsertChangeNotification bulkInsertChangeNotification) {
    EventHelper.invoke(onBulkInsertChangeNotification, bulkInsertChangeNotification);
  }

  public void send(DataSubscriptionChangeNotification dataSubscriptionChangeNotification) {
    EventHelper.invoke(onDataSubscriptionNotification, dataSubscriptionChangeNotification);
  }

}
