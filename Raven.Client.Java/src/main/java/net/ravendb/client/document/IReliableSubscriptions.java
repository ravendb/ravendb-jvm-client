package net.ravendb.client.document;

import java.io.IOException;
import java.util.List;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.SubscriptionConfig;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.data.SubscriptionCriteria;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionException;
import net.ravendb.abstractions.json.linq.RavenJObject;


public interface IReliableSubscriptions extends CleanCloseable {

  /**
   * It creates a data subscription in a database. The subscription will expose all documents that match the specified subscription criteria for a given type.
   * @return Created subscription identifier.
   */
  long create(Class<?> expectedType, SubscriptionCriteria criteria);

  /**
   * It creates a data subscription in a database. The subscription will expose all documents that match the specified subscription criteria for a given type.
   * @return Created subscription identifier.
   */
  long create(Class<?> expectedType, SubscriptionCriteria criteria, String database);

  /**
   * It creates a data subscription in a database. The subscription will expose all documents that match the specified subscription criteria.
   * @return Created subscription identifier.
   */
  long create(SubscriptionCriteria criteria);

  /**
   * It creates a data subscription in a database. The subscription will expose all documents that match the specified subscription criteria.
   * @return Created subscription identifier.
   */
  long create(SubscriptionCriteria criteria, String database);

  /**
   * It opens a subscription and starts pulling documents since a last processed document for that subscription (in document's Etag order).
   * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
   * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
   * There can be only a single client that is connected to a subscription.
   * @return Subscription object that allows to add/remove subscription handlers.
   */
  <T> Subscription<T> open(Class<T> clazz, long id, SubscriptionConnectionOptions options) throws SubscriptionException;

  /**
   * It opens a subscription and starts pulling documents since a last processed document for that subscription (in document's Etag order).
   * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
   * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
   * There can be only a single client that is connected to a subscription.
   * @return Subscription object that allows to add/remove subscription handlers.
   */
  <T> Subscription<T> open(Class<T> clazz, long id, SubscriptionConnectionOptions options, String database) throws SubscriptionException;

  /**
   * It opens a subscription and starts pulling documents since a last processed document for that subscription (in document's Etag order).
   * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
   * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
   * There can be only a single client that is connected to a subscription.
   * @return Subscription object that allows to add/remove subscription handlers.
   */
  Subscription<RavenJObject> open(long id, SubscriptionConnectionOptions options) throws SubscriptionException;

  /**
   * It opens a subscription and starts pulling documents since a last processed document for that subscription (in document's Etag order).
   * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
   * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
   * There can be only a single client that is connected to a subscription.
   * @return Subscription object that allows to add/remove subscription handlers.
   */
  Subscription<RavenJObject> open(long id, SubscriptionConnectionOptions options, String database) throws SubscriptionException;

  /**
   * It downloads a list of all existing subscriptions in a database.
   * @return Existing subscriptions' configurations.
   */
  List<SubscriptionConfig> getSubscriptions(int start, int take) throws IOException;

  /**
   * It downloads a list of all existing subscriptions in a database.
   * @return Existing subscriptions' configurations.
   */
  List<SubscriptionConfig> getSubscriptions(int start, int take, String database) throws IOException;

  /**
   * It deletes a subscription.
   */
  void delete(long id);

  /**
   * It deletes a subscription.
   */
  void delete(long id, String database);

  /**
   * It releases a subscriptions by forcing a connected client to drop.
   */
  void release(long id);

  /**
   * It releases a subscriptions by forcing a connected client to drop.
   */
  void release(long id, String database);
}
