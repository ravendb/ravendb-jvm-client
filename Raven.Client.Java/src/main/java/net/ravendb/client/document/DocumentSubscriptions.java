package net.ravendb.client.document;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.SubscriptionConfig;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.data.SubscriptionCriteria;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionClosedException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistExeption;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionException;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionInUseException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.profiling.ConcurrentSet;


public class DocumentSubscriptions implements IReliableSubscriptions {
  private final IDocumentStore documentStore;
  private final ConcurrentSet<CleanCloseable> subscriptions  = new ConcurrentSet<>();

  public DocumentSubscriptions(IDocumentStore documentStore) {
    super();
    this.documentStore = documentStore;
  }

  @Override
  public long create(Class<?> expectedType, SubscriptionCriteria criteria) {
    return create(expectedType, criteria, null);
  }

  @Override
  public long create(Class<?> expectedType, SubscriptionCriteria criteria, String database) {
    if (criteria == null) {
      throw new IllegalArgumentException("Cannot create a subscription if criteria is null");
    }

    SubscriptionCriteria criteriaCopy = new SubscriptionCriteria();
    criteriaCopy.setBelongsToCollection(documentStore.getConventions().getTypeTagName(expectedType));
    criteriaCopy.setKeyStartsWith(criteria.getKeyStartsWith());
    criteriaCopy.setPropertiesMatch(criteria.getPropertiesMatch());
    criteriaCopy.setPropertiesNotMatch(criteria.getPropertiesNotMatch());

    return create(criteria, database);
  }

  @Override
  public long create(SubscriptionCriteria criteria) {
    return create(criteria, null);
  }

  @SuppressWarnings("boxing")
  @Override
  public long create(SubscriptionCriteria criteria, String database) {
    if (criteria == null) {
      throw new IllegalArgumentException("Cannot create a subscription if criteria is null");
    }

    IDatabaseCommands commands = database == null
      ? documentStore.getDatabaseCommands()
        : documentStore.getDatabaseCommands().forDatabase(database);

    try (HttpJsonRequest request = commands.createRequest(HttpMethods.POST, "/subscriptions/create")) {
      request.write(RavenJObject.fromObject(criteria).toString());
      return request.readResponseJson().value(Long.class, "Id");
    }
  }

  @Override
  public <T> Subscription<T> open(Class<T> clazz, long id, SubscriptionConnectionOptions options) throws SubscriptionException {
    return open(clazz, id, options, null);
  }

  @Override
  public <T> Subscription<T> open(Class<T> clazz, final long id, final SubscriptionConnectionOptions options, String database) throws SubscriptionException {
    if (options == null) {
      throw new IllegalArgumentException("Cannot open a subscription if options are null");
    }

    if (options.getBatchOptions() == null) {
      throw new IllegalArgumentException("Cannot open a subscription if batch options are null");
    }

    if (options.getBatchOptions().getMaxSize() != null && options.getBatchOptions().getMaxSize() < 16 * 1024) {
      throw new IllegalArgumentException("Max size value of batch options cannot be lower than that 16 KB");
    }

    final IDatabaseCommands commands = database == null
      ? documentStore.getDatabaseCommands()
        : documentStore.getDatabaseCommands().forDatabase(database);

    sendOpenSubscriptionRequest(commands, id, options);

    Subscription<T> subscription = new Subscription<>(clazz, id, options, commands, documentStore.changes(database), documentStore.getConventions(),
      new Action0() {
        @Override
        public void apply() {
          sendOpenSubscriptionRequest(commands, id, options); // to ensure that subscription is open try to call it with the same connection id
        }
      });

    subscriptions.add(subscription);
    return subscription;
  }

  @Override
  public Subscription<RavenJObject> open(long id, SubscriptionConnectionOptions options)  throws SubscriptionException {
    return open(RavenJObject.class, id, options, null);
  }

  @Override
  public Subscription<RavenJObject> open(long id, SubscriptionConnectionOptions options, String database)  throws SubscriptionException {
    return open(RavenJObject.class, id, options, database);
  }

  private static void sendOpenSubscriptionRequest(IDatabaseCommands commands, long id, SubscriptionConnectionOptions options) throws SubscriptionException {
    try (HttpJsonRequest request = commands.createRequest(HttpMethods.POST, String.format("/subscriptions/open?id=%d&connection=%s", id, options.getConnectionId()))) {
      request.write(options.toRavenObject().toString());
      request.executeRequest();
    } catch (Exception e) {
      SubscriptionException subscriptionException = tryGetSubscriptionException(e);
      if (subscriptionException != null) {
        throw subscriptionException;
      }
      throw e;
    }
  }

  @Override
  public List<SubscriptionConfig> getSubscriptions(int start, int take) throws IOException {
    return getSubscriptions(start, take, null);
  }

  @Override
  public List<SubscriptionConfig> getSubscriptions(int start, int take, String database) {
    IDatabaseCommands commands = database == null
      ? documentStore.getDatabaseCommands()
        : documentStore.getDatabaseCommands().forDatabase(database);

      try (HttpJsonRequest request = commands.createRequest(HttpMethods.GET, "/subscriptions")) {
        RavenJToken response = request.readResponseJson();
        SubscriptionConfig[] subscriptionConfigs = documentStore.getConventions().createSerializer().deserialize(response, SubscriptionConfig[].class);
        return Arrays.asList(subscriptionConfigs);
      }
  }

  @Override
  public void delete(long id) {
    delete(id, null);
  }

  @Override
  public void delete(long id, String database) {
    IDatabaseCommands commands = database == null
      ? documentStore.getDatabaseCommands()
        : documentStore.getDatabaseCommands().forDatabase(database);

      try (HttpJsonRequest request = commands.createRequest(HttpMethods.DELETE, "/subscriptions?id=" + id)) {
        request.executeRequest();
      }
  }

  @Override
  public void release(long id) {
    release(id, null);
  }

  @Override
  public void release(long id, String database) {
    IDatabaseCommands commands = database == null
      ? documentStore.getDatabaseCommands()
        : documentStore.getDatabaseCommands().forDatabase(database);

      try(HttpJsonRequest request = commands.createRequest(HttpMethods.POST, String.format("/subscriptions/close?id=%d&connection=&force=true", id))) {
        request.executeRequest();
      }
  }

  public static SubscriptionException tryGetSubscriptionException(Exception ere) {
    if (ere instanceof ErrorResponseException) {
      ErrorResponseException opException = (ErrorResponseException) ere;

      try {
        String cause = opException.getResponseString();

        if (opException.getStatusCode() == SubscriptionDoesNotExistExeption.RELEVANT_HTTP_STATUS_CODE) {
          return new SubscriptionDoesNotExistExeption(cause);
        }

        if (opException.getStatusCode() == SubscriptionInUseException.RELEVANT_HTTP_STATUS_CODE) {
          return new SubscriptionInUseException(cause);
        }

        if (opException.getStatusCode() == SubscriptionClosedException.RELEVANT_HTTP_STATUS_CODE) {
          return new SubscriptionClosedException(cause);
        }
      } catch (Exception e) {
        return null;
      }

    }
    return null;
  }

  @Override
  public void close() {
    for (CleanCloseable closeable: subscriptions) {
      closeable.close();
    }
  }

}
