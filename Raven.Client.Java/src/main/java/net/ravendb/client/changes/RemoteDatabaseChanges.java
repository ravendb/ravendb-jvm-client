package net.ravendb.client.changes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import net.ravendb.abstractions.basic.ExceptionEventArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function4;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.closure.Predicates;
import net.ravendb.abstractions.data.*;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.util.AtomicDictionary;
import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.JsonSerializer;
import net.ravendb.client.utils.UrlUtils;



public class RemoteDatabaseChanges extends RemoteChangesClientBase<IDatabaseChanges, DatabaseConnectionState> implements IDatabaseChanges {

  protected final ConcurrentSkipListSet<String> watchedDocs = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedPrefixes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedTypes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedCollections = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedIndexes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedBulkInserts = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<Long> watchedDataSubscriptions = new ConcurrentSkipListSet<>();
  protected boolean watchAllDocs;
  protected boolean watchAllIndexes;
  protected boolean watchAllTransformers;
  protected boolean watchAllDataSubscriptions;
  @SuppressWarnings("hiding")
  protected DocumentConvention conventions;

  private final Function4<String, Etag, String[] , OperationMetadata, Boolean> tryResolveConflictByUsingRegisteredConflictListeners;


  public RemoteDatabaseChanges(String url, String apiKey, HttpJsonRequestFactory jsonRequestFactory, DocumentConvention conventions,
    IDocumentStoreReplicationInformer replicationInformer, Action0 onDispose,
    Function4<String, Etag, String[], OperationMetadata, Boolean> tryResolveConflictByUsingRegisteredConflictListeners) {
    super(url, apiKey, jsonRequestFactory, conventions, replicationInformer, onDispose);
    this.conventions = conventions;
    this.tryResolveConflictByUsingRegisteredConflictListeners = tryResolveConflictByUsingRegisteredConflictListeners;
  }


  @Override
  protected void subscribeOnServer() {

    if (watchAllDocs) {
      send("watch-docs", null);
    }
    if (watchAllIndexes) {
      send("watch-indexes", null);
    }
    if (watchAllTransformers) {
      send("watch-transformers", null);
    }
    if (watchAllDataSubscriptions) {
      send("watch-data-subscriptions", null);
    }
    if (watchedDocs != null) {
      for (String watchedDoc : watchedDocs) {
        send("watch-doc", watchedDoc);
      }
    }
    if (watchedPrefixes != null) {
      for (String watchedPrefix : watchedPrefixes) {
        send("watch-prefix", watchedPrefix);
      }
    }
    if (watchedCollections != null) {
      for (String watchedCollection : watchedCollections) {
        send("watch-collection", watchedCollection);
      }
    }
    if (watchedTypes != null) {
      for (String watchedType : watchedTypes) {
        send("watch-type", watchedType);
      }
    }
    if (watchedIndexes != null) {
      for (String watchedIndex : watchedIndexes) {
        send("watch-indexes", watchedIndex);
      }
    }
    if (watchedBulkInserts != null) {
      for (String watchedBulkInsert : watchedBulkInserts) {
        send("watch-bulk-operation", watchedBulkInsert);
      }
    }
  }


  @SuppressWarnings({"hiding", "boxing"})
  @Override
  protected void notifySubscribers(String type, RavenJObject value, List<DatabaseConnectionState> connections) {
    JsonSerializer serializer = new JsonSerializer();
    switch (type) {
      case "DocumentChangeNotification":
        DocumentChangeNotification documentChangeNotification = serializer.deserialize(value.toString(), DocumentChangeNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(documentChangeNotification);
        }
        break;

      case "BulkInsertChangeNotification":
        BulkInsertChangeNotification bulkInsertChangeNotification = serializer.deserialize(value.toString(), BulkInsertChangeNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(bulkInsertChangeNotification);
        }
        break;

      case "IndexChangeNotification":
        IndexChangeNotification indexChangeNotification = serializer.deserialize(value.toString(), IndexChangeNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(indexChangeNotification);
        }
        break;
      case "TransformerChangeNotification":
        TransformerChangeNotification transformerChangeNotification = serializer.deserialize(value.toString(), TransformerChangeNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(transformerChangeNotification);
        }
        break;
      case "ReplicationConflictNotification":
        ReplicationConflictNotification replicationConflictNotification = serializer.deserialize(value.toString(), ReplicationConflictNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(replicationConflictNotification);
        }
        if (replicationConflictNotification.getItemType().equals(ReplicationConflictTypes.DOCUMENT_REPLICATION_CONFLICT)) {
          boolean result = tryResolveConflictByUsingRegisteredConflictListeners.apply(replicationConflictNotification.getId(),
            replicationConflictNotification.getEtag(), replicationConflictNotification.getConflicts(), null);
          if (result) {
            logger.debug("Document replication conflict for %s was resolved by one of the registered conflict listeners",
              replicationConflictNotification.getId());
          }
        }
        break;
      case "DataSubscriptionChangeNotification":
        DataSubscriptionChangeNotification dataSubscriptionChangeNotification = serializer.deserialize(value.toString(), DataSubscriptionChangeNotification.class);
        for (DatabaseConnectionState counter : connections) {
          counter.send(dataSubscriptionChangeNotification);
        }
        break;
      default:
        break;
    }
  }


  @Override
  public IObservable<IndexChangeNotification> forIndex(final String indexName) {

    DatabaseConnectionState counter = getOrAddConnectionState("indexes/" + indexName, "watch-index", "unwatch-index", new Action0() {
      @Override
      public void apply() {
        watchedIndexes.add(indexName);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedIndexes.remove(indexName);
      }
    }, indexName);
    counter.inc();
    final TaskedObservable<IndexChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<IndexChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(IndexChangeNotification notification) {
        return notification.getName().equalsIgnoreCase(indexName);
      }
    });

    counter.getOnIndexChangeNotification().add(new Action1<IndexChangeNotification>() {
      @Override
      public void apply(IndexChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocument(final String docId) {
    DatabaseConnectionState counter = getOrAddConnectionState("docs/" + docId, "watch-doc", "unwatch-doc", new Action0() {
      @Override
      public void apply() {
        watchedDocs.add(docId);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedDocs.remove(docId);
      }
    }, docId);

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(DocumentChangeNotification notification) {
        return notification.getId().equalsIgnoreCase(docId);
      }
    });

    counter.getOnDocumentChangeNotification().add(new Action1<DocumentChangeNotification>() {
      @Override
      public void apply(DocumentChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forAllDocuments() {
    DatabaseConnectionState counter = getOrAddConnectionState("all-docs", "watch-docs", "unwatch-docs", new Action0() {
      @Override
      public void apply() {
        watchAllDocs = true;
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchAllDocs = false;
      }
    }, null);

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, Predicates.<DocumentChangeNotification> alwaysTrue());

    counter.getOnDocumentChangeNotification().add(new Action1<DocumentChangeNotification>() {
      @Override
      public void apply(DocumentChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<BulkInsertChangeNotification> forBulkInsert() {
    return forBulkInsert(null);
  }

  @Override
  public IObservable<BulkInsertChangeNotification> forBulkInsert(final UUID operationId) {

    final String id = operationId != null ? operationId.toString() : "";

    DatabaseConnectionState counter = counters.getOrAdd("bulk-operations/" + id, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchedBulkInserts.add(id);
        send("watch-bulk-operation", id);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedBulkInserts.remove(id);
            send("unwatch-bulk-operation", id);
            counters.remove("bulk-operations/" + operationId);
          }
        }, new Action1<DatabaseConnectionState>() {
          @Override
          public void apply(final DatabaseConnectionState existingConnectionState) {
            if (counters.get("bulk-operations/" + id) != null) {
              return;
            }

            counters.getOrAdd("bulk-operations/" + id, new Function1<String, DatabaseConnectionState>() {
              @Override
              public DatabaseConnectionState apply(String input) {
                return existingConnectionState;
              }
            });

            send("watch-bulk-operation", id);
          }
        });
      }
    });

    final TaskedObservable<BulkInsertChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<BulkInsertChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(BulkInsertChangeNotification notification) {
        return operationId == null || notification.getOperationId().equals(operationId);
      }
    });

    counter.getOnBulkInsertChangeNotification().add(new Action1<BulkInsertChangeNotification>() {
      @Override
      public void apply(BulkInsertChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DataSubscriptionChangeNotification> forAllDataSubscriptions() {
    DatabaseConnectionState counter = getOrAddConnectionState("all-data-subscriptions", "watch-data-subscriptions", "unwatch-data-subscriptions", new Action0() {
      @Override
      public void apply() {
        watchAllDataSubscriptions = true;
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchAllDataSubscriptions = false;
      }
    }, null);
    final TaskedObservable<DataSubscriptionChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, Predicates.<DataSubscriptionChangeNotification> alwaysTrue());

    counter.getOnDataSubscriptionNotification().add(new Action1<DataSubscriptionChangeNotification>() {
      @Override
      public void apply(DataSubscriptionChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DataSubscriptionChangeNotification> forDataSubscription(final long subscriptionId) {
    DatabaseConnectionState counter = getOrAddConnectionState("subscriptions/" + subscriptionId, "watch-data-subscription", "unwatch-data-subscription", new Action0() {
      @Override
      public void apply() {
        watchedDataSubscriptions.add(subscriptionId);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedDataSubscriptions.remove(subscriptionId);
      }
    }, String.valueOf(subscriptionId));
    final TaskedObservable<DataSubscriptionChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DataSubscriptionChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(DataSubscriptionChangeNotification notification) {
        return notification.getId() == subscriptionId;
      }
    });

    counter.getOnDataSubscriptionNotification().add(new Action1<DataSubscriptionChangeNotification>() {
      @Override
      public void apply(DataSubscriptionChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  private DatabaseConnectionState getOrAddConnectionState(final String name, final String watchCommand,
                                                          final String unwatchCommand, final Action0 afterConnection,
                                                          final Action0 beforeDisconnect, final String value) {
    DatabaseConnectionState counter = counters.getOrAdd(name, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        afterConnection.apply();
        send(watchCommand, value);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            beforeDisconnect.apply();
            send(unwatchCommand, value);
            counters.remove(name);
          }
        }, new Action1<DatabaseConnectionState>() {
          @Override
          public void apply(final DatabaseConnectionState existingConnectionState) {
            if (counters.get(name) != null) {
              return;
            }
            counters.getOrAdd(name, new Function1<String, DatabaseConnectionState>() {
                      @Override
                      public DatabaseConnectionState apply(String input) {
                        return existingConnectionState;
                      }
                    }
            );

            afterConnection.apply();
            send(watchCommand, value);
          }
        });
      }

    });
    return counter;
  }

  @Override
  public IObservable<IndexChangeNotification> forAllIndexes() {
    DatabaseConnectionState counter = getOrAddConnectionState("all-indexes", "watch-indexes", "unwatch-indxes", new Action0() {
      @Override
      public void apply() {
        watchAllIndexes = true;
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchAllIndexes = false;
      }
    }, null);

    counter.inc();
    final TaskedObservable<IndexChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, Predicates.<IndexChangeNotification> alwaysTrue());

    counter.getOnIndexChangeNotification().add(new Action1<IndexChangeNotification>() {
      @Override
      public void apply(IndexChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<TransformerChangeNotification> forAllTransformers() {
    DatabaseConnectionState counter = getOrAddConnectionState("all-transformers", "watch-transformers", "unwatch-transformers", new Action0() {
      @Override
      public void apply() {
        watchAllTransformers = true;
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchAllTransformers = false;
      }
    }, null);
    counter.inc();
    final TaskedObservable<TransformerChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, Predicates.<TransformerChangeNotification> alwaysTrue());

    counter.getOnTransformerChangeNotification().add(new Action1<TransformerChangeNotification>() {
      @Override
      public void apply(TransformerChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsStartingWith(final String docIdPrefix) {
    DatabaseConnectionState counter = getOrAddConnectionState("prefixes/" + docIdPrefix, "watch-prefix", "unwatch-prefix", new Action0() {
      @Override
      public void apply() {
        watchedPrefixes.add(docIdPrefix);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedPrefixes.remove(docIdPrefix);
      }
    }, docIdPrefix);

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(DocumentChangeNotification notification) {
        return notification.getId() != null && notification.getId().toLowerCase().startsWith(docIdPrefix.toLowerCase());
      }
    });

    counter.getOnDocumentChangeNotification().add(new Action1<DocumentChangeNotification>() {
      @Override
      public void apply(DocumentChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(final String collectionName) {
    if (collectionName == null) {
      throw new IllegalArgumentException("Collection name is null");
    }
    DatabaseConnectionState counter = getOrAddConnectionState("collections/" + collectionName, "watch-collection", "unwatch-collection", new Action0() {
      @Override
      public void apply() {
        watchedCollections.add(collectionName);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedCollections.remove(collectionName);
      }
    }, collectionName);

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(DocumentChangeNotification notification) {
        return notification.getCollectionName() != null &&  notification.getCollectionName().equalsIgnoreCase(collectionName);
      }
    });

    counter.getOnDocumentChangeNotification().add(new Action1<DocumentChangeNotification>() {
      @Override
      public void apply(DocumentChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsInCollection(Class<?> clazz) {
    String collectionName = conventions.getTypeTagName(clazz);
    return forDocumentsInCollection(collectionName);
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsOfType(final String typeName) {
    if (typeName == null) {
      throw new IllegalArgumentException("TypeName name is null");
    }
    final String encodedTypeName = UrlUtils.escapeDataString(typeName);

    DatabaseConnectionState counter = getOrAddConnectionState("types/" + typeName, "watch-type", "unwatch-type", new Action0() {
      @Override
      public void apply() {
        watchedTypes.add(typeName);
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchedTypes.remove(typeName);
      }
    }, encodedTypeName);

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean apply(DocumentChangeNotification notification) {
        return notification.getTypeName() != null &&  notification.getTypeName().equalsIgnoreCase(typeName);
      }
    });

    counter.getOnDocumentChangeNotification().add(new Action1<DocumentChangeNotification>() {
      @Override
      public void apply(DocumentChangeNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public IObservable<DocumentChangeNotification> forDocumentsOfType(Class<?> clazz) {
    String typeName = conventions.getFindJavaClassName().find(clazz);
    return forDocumentsOfType(typeName);
  }


  @Override
  public IObservable<ReplicationConflictNotification> forAllReplicationConflicts() {
    DatabaseConnectionState counter = getOrAddConnectionState("all-replication-conflicts", "watch-replication-conflicts", "unwatch-replication-conflicts", new Action0() {
      @Override
      public void apply() {
        watchAllIndexes = true;
      }
    }, new Action0() {
      @Override
      public void apply() {
        watchAllIndexes = false;
      }
    }, null);

    final TaskedObservable<ReplicationConflictNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, Predicates.<ReplicationConflictNotification> alwaysTrue());

    counter.getOnReplicationConflictNotification().add(new Action1<ReplicationConflictNotification>() {
      @Override
      public void apply(ReplicationConflictNotification msg) {
        taskedObservable.send(msg);
      }
    });
    counter.getOnError().add(new Action1<ExceptionEventArgs>() {
      @Override
      public void apply(ExceptionEventArgs ex) {
        taskedObservable.error(ex.getException());
      }
    });
    return taskedObservable;
  }

  @Override
  public void waitForAllPendingSubscriptions() {
    // this method simply returns as we process requests synchronically
  }

}
