package net.ravendb.client.changes;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import net.ravendb.abstractions.basic.ExceptionEventArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function4;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.closure.Predicates;
import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.DocumentChangeNotification;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexChangeNotification;
import net.ravendb.abstractions.data.ReplicationConflictNotification;
import net.ravendb.abstractions.data.ReplicationConflictTypes;
import net.ravendb.abstractions.data.TransformerChangeNotification;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.util.AtomicDictionary;
import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.ReflectionUtil;
import net.ravendb.client.utils.UrlUtils;

import org.codehaus.jackson.map.ObjectMapper;



public class RemoteDatabaseChanges extends RemoteChangesClientBase<IDatabaseChanges, DatabaseConnectionState> implements IDatabaseChanges {

  protected final ConcurrentSkipListSet<String> watchedDocs = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedPrefixes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedTypes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedCollections = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedIndexes = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<String> watchedBulkInserts = new ConcurrentSkipListSet<>();
  protected boolean watchAllDocs;
  protected boolean watchAllIndexes;
  protected boolean watchAllTransformers;
  protected DocumentConvention conventions;

  private final Function4<String, Etag, String[] , OperationMetadata, Boolean> tryResolveConflictByUsingRegisteredConflictListeners;


  public RemoteDatabaseChanges(String url, String apiKey, HttpJsonRequestFactory jsonRequestFactory, DocumentConvention conventions,
    IDocumentStoreReplicationInformer replicationInformer, Action0 onDispose,
    Function4<String, Etag, String[], OperationMetadata, Boolean> tryResolveConflictByUsingRegisteredConflictListeners) {
    super(url, apiKey, jsonRequestFactory, conventions, replicationInformer, onDispose);
    subscribeOnServer();
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
    for (String watchedDoc : watchedDocs) {
      send("watch-doc", watchedDoc);
    }
    for (String watchedPrefix : watchedPrefixes) {
      send("watch-prefix", watchedPrefix);
    }
    for (String watchedCollection : watchedCollections) {
      send("watch-collection", watchedCollection);
    }
    for (String watchedType : watchedTypes) {
      send("watch-type", watchedType);
    }
    for (String watchedIndex : watchedIndexes) {
      send("watch-indexes", watchedIndex);
    }
    for (String watchedBulkInsert : watchedBulkInserts) {
      send("watch-bulk-operation", watchedBulkInsert);
    }
  }


  @Override
  protected void notifySubscribers(String type, RavenJObject value, AtomicDictionary<DatabaseConnectionState> counters) {
    try {
      ObjectMapper mapper = JsonExtensions.createDefaultJsonSerializer();
      switch (type) {
        case "DocumentChangeNotification":
          DocumentChangeNotification documentChangeNotification = mapper.readValue(value.toString(), DocumentChangeNotification.class);
          for (DatabaseConnectionState counter : counters.values()) {
            counter.send(documentChangeNotification);
          }
          break;

        case "BulkInsertChangeNotification":
          BulkInsertChangeNotification bulkInsertChangeNotification = mapper.readValue(value.toString(), BulkInsertChangeNotification.class);
          for (DatabaseConnectionState counter : counters.values()) {
            counter.send(bulkInsertChangeNotification);
          }
          break;

        case "IndexChangeNotification":
          IndexChangeNotification indexChangeNotification = mapper.readValue(value.toString(), IndexChangeNotification.class);
          for (DatabaseConnectionState counter : counters.values()) {
            counter.send(indexChangeNotification);
          }
          break;
        case "TransformerChangeNotification":
          TransformerChangeNotification transformerChangeNotification = mapper.readValue(value.toString(), TransformerChangeNotification.class);
          for (DatabaseConnectionState counter : counters.values()) {
            counter.send(transformerChangeNotification);
          }
          break;
        case "ReplicationConflictNotification":
          ReplicationConflictNotification replicationConflictNotification = mapper.readValue(value.toString(), ReplicationConflictNotification.class);
          for (DatabaseConnectionState counter: counters.values()) {
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
        default:
          break;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public IObservable<IndexChangeNotification> forIndex(final String indexName) {
    DatabaseConnectionState counter = counters.getOrAdd("indexes/" + indexName, new Function1<String, DatabaseConnectionState>() {

      @Override
      public DatabaseConnectionState apply(String s) {
        watchedIndexes.add(indexName);
        send("watch-index", indexName);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedIndexes.remove(indexName);
            send("unwatch-index", indexName);
            counters.remove("indexes/" + indexName);
          }
        });
      }
    });
    counter.inc();
    final TaskedObservable<IndexChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<IndexChangeNotification>() {
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
    DatabaseConnectionState counter = counters.getOrAdd("docs/" + docId, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchedDocs.add(docId);
        send("watch-doc", docId);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedDocs.remove(docId);
            send("unwatch-doc", docId);
            counters.remove("docs/" + docId);
          }
        });
      }
    });

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
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
    DatabaseConnectionState counter = counters.getOrAdd("all-docs", new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchAllDocs = true;
        send("watch-docs", null);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchAllDocs = false;
            send("unwatch-docs", null);
            counters.remove("all-docs");
          }
        });
      }
    });

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
        });
      }
    });

    final TaskedObservable<BulkInsertChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<BulkInsertChangeNotification>() {
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
  public IObservable<IndexChangeNotification> forAllIndexes() {
    DatabaseConnectionState counter = counters.getOrAdd("all-indexes", new Function1<String, DatabaseConnectionState>() {

      @Override
      public DatabaseConnectionState apply(String s) {
        watchAllIndexes = true;
        send("watch-indexes", null);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchAllIndexes = false;
            send("unwatch-indexes", null);
            counters.remove("all-indexes");
          }
        });
      }
    });
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
    DatabaseConnectionState counter = counters.getOrAdd("all-transformers", new Function1<String, DatabaseConnectionState>() {

      @Override
      public DatabaseConnectionState apply(String s) {
        watchAllTransformers = true;
        send("watch-transformers", null);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchAllTransformers = false;
            send("unwatch-transformers", null);
            counters.remove("all-transformers");
          }
        });
      }
    });
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
    DatabaseConnectionState counter = counters.getOrAdd("prefixes" + docIdPrefix, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchedPrefixes.add(docIdPrefix);
        send("watch-prefix", docIdPrefix);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedPrefixes.remove(docIdPrefix);
            send("unwatch-prefix", docIdPrefix);
            counters.remove("prefixes/" + docIdPrefix);
          }
        });
      }
    });

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
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
    DatabaseConnectionState counter = counters.getOrAdd("collections/" + collectionName, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchedCollections.add(collectionName);
        send("watch-collection", collectionName);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedCollections.remove(collectionName);
            send("unwatch-collection", collectionName);
            counters.remove("collections/" + collectionName);
          }
        });
      }
    });

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
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

    DatabaseConnectionState counter = counters.getOrAdd("types/" + typeName, new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchedTypes.add(typeName);
        send("watch-type", encodedTypeName);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchedTypes.remove(typeName);
            send("unwatch-type", encodedTypeName);
            counters.remove("types/" + typeName);
          }
        });
      }
    });

    final TaskedObservable<DocumentChangeNotification, DatabaseConnectionState> taskedObservable = new TaskedObservable<>(counter, new Predicate<DocumentChangeNotification>() {
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
    DatabaseConnectionState counter = counters.getOrAdd("all-replication-conflicts", new Function1<String, DatabaseConnectionState>() {
      @Override
      public DatabaseConnectionState apply(String s) {
        watchAllIndexes = true;
        send("watch-replication-conflicts", null);

        return new DatabaseConnectionState(new Action0() {
          @Override
          public void apply() {
            watchAllIndexes = false;
            send("unwatch-replication-conflicts", null);
            counters.remove("all-replication-conflicts");
          }
        });
      }
    });

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
