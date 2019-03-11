package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentSubscriptions implements AutoCloseable {

    private final DocumentStore _store;
    private final ConcurrentHashMap<CleanCloseable, Boolean> _subscriptions = new ConcurrentHashMap<>();

    public DocumentSubscriptions(DocumentStore store) {
        _store = store;
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @return created subscription
     */
    public String create(SubscriptionCreationOptions options) {
        return create(options, null);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @param database Target database
     * @return created subscription
     */
    public String create(SubscriptionCreationOptions options, String database) {
        if (options == null) {
            throw new IllegalArgumentException("Cannot create a subscription if options is null");
        }

        if (options.getQuery() == null) {
            throw new IllegalArgumentException("Cannot create a subscription if the script is null");
        }

        RequestExecutor requestExecutor = _store.getRequestExecutor(ObjectUtils.firstNonNull(database, _store.getDatabase()));

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(_store.getConventions(), options);
        requestExecutor.execute(command);

        return command.getResult().getName();
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param clazz Document class
     * @param <T> Document class
     * @return created subscription
     */
    public <T> String create(Class<T> clazz) {
        return create(clazz, null, null);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @param clazz Document class
     * @param <T> Document class
     * @return created subscription
     */
    public <T> String create(Class<T> clazz, SubscriptionCreationOptions options) {
        return create(clazz, options, null);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @param clazz Document class
     * @param <T> Document class
     * @param database Target database
     * @return created subscription
     */
    public <T> String create(Class<T> clazz, SubscriptionCreationOptions options, String database) {
        options = ObjectUtils.firstNonNull(options, new SubscriptionCreationOptions());

        return create(ensureCriteria(options, clazz, false), database);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param <T> Document class
     * @param clazz Document class
     * @return created subscription
     */
    public <T> String createForRevisions(Class<T> clazz) {
        return createForRevisions(clazz, null, null);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @param clazz Document class
     * @param <T> Document class
     * @return created subscription
     */
    public <T> String createForRevisions(Class<T> clazz, SubscriptionCreationOptions options) {
        return createForRevisions(clazz, options, null);
    }

    /**
     * Creates a data subscription in a database. The subscription will expose all documents that match the specified subscription options for a given type.
     * @param options Subscription options
     * @param clazz Document class
     * @param <T> Document class
     * @param database Target database
     * @return created subscription
     */
    public <T> String createForRevisions(Class<T> clazz, SubscriptionCreationOptions options, String database) {
        options = ObjectUtils.firstNonNull(options, new SubscriptionCreationOptions());
        return create(ensureCriteria(options, clazz, true), database);
    }

    private <T> SubscriptionCreationOptions ensureCriteria(SubscriptionCreationOptions criteria, Class<T> clazz, boolean revisions) {
        if (criteria == null) {
            criteria = new SubscriptionCreationOptions();
        }

        String collectionName = _store.getConventions().getCollectionName(clazz);
        if (criteria.getQuery() == null) {

            if (revisions) {
                criteria.setQuery("from " + collectionName + " (Revisions = true) as doc");
            } else {
                criteria.setQuery("from " + collectionName + " as doc");
            }
        }

        return criteria;
    }


    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param options Subscription options
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public SubscriptionWorker<ObjectNode> getSubscriptionWorker(SubscriptionWorkerOptions options) {
        return getSubscriptionWorker(options, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param options Subscription options
     * @param database Target database
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public SubscriptionWorker<ObjectNode> getSubscriptionWorker(SubscriptionWorkerOptions options, String database) {
        return getSubscriptionWorker(ObjectNode.class, options, database);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param subscriptionName The name of subscription
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public SubscriptionWorker<ObjectNode> getSubscriptionWorker(String subscriptionName) {
        return getSubscriptionWorker(ObjectNode.class, subscriptionName, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param subscriptionName The name of subscription
     * @param database Target database
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public SubscriptionWorker<ObjectNode> getSubscriptionWorker(String subscriptionName, String database) {
        return getSubscriptionWorker(ObjectNode.class, subscriptionName, database);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param options Subscription options
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<T> getSubscriptionWorker(Class<T> clazz, SubscriptionWorkerOptions options) {
        return getSubscriptionWorker(clazz, options, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param options Subscription options
     * @param database Target database
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<T> getSubscriptionWorker(Class<T> clazz, SubscriptionWorkerOptions options, String database) {
        _store.assertInitialized();
        if (options == null) {
            throw new IllegalStateException("Cannot open a subscription if options are null");
        }

        SubscriptionWorker<T> subscription = new SubscriptionWorker<>(clazz, options, false, _store, database);

        subscription.onClosed = sender -> _subscriptions.remove(sender);
        _subscriptions.put(subscription, true);

        return subscription;
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param subscriptionName The name of subscription
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<T> getSubscriptionWorker(Class<T> clazz, String subscriptionName) {
        return getSubscriptionWorker(clazz, subscriptionName, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param subscriptionName The name of subscription
     * @param database Target database
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<T> getSubscriptionWorker(Class<T> clazz, String subscriptionName, String database) {
        return getSubscriptionWorker(clazz, new SubscriptionWorkerOptions(subscriptionName), database);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param options Subscription options
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<Revision<T>> getSubscriptionWorkerForRevisions(Class<T> clazz, SubscriptionWorkerOptions options) {
        return getSubscriptionWorkerForRevisions(clazz, options, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param options Subscription options
     * @param database Target database
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<Revision<T>> getSubscriptionWorkerForRevisions(Class<T> clazz, SubscriptionWorkerOptions options, String database) {
        SubscriptionWorker<Revision<T>> subscription = new SubscriptionWorker<>(clazz, options, true, _store, database);

        subscription.onClosed = sender -> _subscriptions.remove(sender);
        _subscriptions.put(subscription, true);

        return subscription;
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param subscriptionName The name of subscription
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<Revision<T>> getSubscriptionWorkerForRevisions(Class<T> clazz, String subscriptionName) {
        return getSubscriptionWorkerForRevisions(clazz, subscriptionName, null);
    }

    /**
     * It opens a subscription and starts pulling documents since a last processed document for that subscription.
     * The connection options determine client and server cooperation rules like document batch sizes or a timeout in a matter of which a client
     * needs to acknowledge that batch has been processed. The acknowledgment is sent after all documents are processed by subscription's handlers.
     *
     * There can be only a single client that is connected to a subscription.
     * @param clazz Entity class
     * @param database Target database
     * @param subscriptionName The name of subscription
     * @param <T> Entity class
     * @return Subscription object that allows to add/remove subscription handlers.
     */
    public <T> SubscriptionWorker<Revision<T>> getSubscriptionWorkerForRevisions(Class<T> clazz, String subscriptionName, String database) {
        return getSubscriptionWorkerForRevisions(clazz, new SubscriptionWorkerOptions(subscriptionName), database);
    }

    /**
     * It downloads a list of all existing subscriptions in a database.
     * @param start Range start
     * @param take Maximum number of items that will be retrieved
     * @return Subscriptions list
     */
    public List<SubscriptionState> getSubscriptions(int start, int take) {
        return getSubscriptions(start, take, null);
    }

    /**
     * It downloads a list of all existing subscriptions in a database.
     *
     * @param start Range start
     * @param take Maximum number of items that will be retrieved
     * @param database Database to use
     * @return List of subscriptions state
     */
    public List<SubscriptionState> getSubscriptions(int start, int take, String database) {
        RequestExecutor requestExecutor = _store.getRequestExecutor(ObjectUtils.firstNonNull(database, _store.getDatabase()));

        GetSubscriptionsCommand command = new GetSubscriptionsCommand(start, take);
        requestExecutor.execute(command);

        return Arrays.asList(command.getResult());
    }

    /**
     * Delete a subscription.
     * @param name Subscription name
     */
    public void delete(String name) {
        delete(name, null);
    }

    /**
     * Delete a subscription.
     *
     * @param name Subscription name
     * @param database Database to use
     */
    public void delete(String name, String database) {
        RequestExecutor requestExecutor = _store.getRequestExecutor(ObjectUtils.firstNonNull(database, _store.getDatabase()));

        DeleteSubscriptionCommand command = new DeleteSubscriptionCommand(name);
        requestExecutor.execute(command);
    }

    /**
     * Returns subscription definition and it's current state
     * @param subscriptionName Subscription name as received from the server
     * @return Subscription state
     */
    public SubscriptionState getSubscriptionState(String subscriptionName) {
        return getSubscriptionState(subscriptionName, null);
    }

    /**
     * Returns subscription definition and it's current state
     * @param subscriptionName Subscription name as received from the server
     * @param database Database to use
     * @return Subscription states
     */
    public SubscriptionState getSubscriptionState(String subscriptionName, String database) {
        if (StringUtils.isEmpty(subscriptionName)) {
            throw new IllegalArgumentException("SubscriptionName cannot be null");
        }

        RequestExecutor requestExecutor = _store.getRequestExecutor(ObjectUtils.firstNonNull(database, _store.getDatabase()));

        GetSubscriptionStateCommand command = new GetSubscriptionStateCommand(subscriptionName);
        requestExecutor.execute(command);
        return command.getResult();
    }

    @Override
    public void close() {
        if (_subscriptions.isEmpty()) {
            return;
        }

        for (CleanCloseable subscription : _subscriptions.keySet()) {
            subscription.close();
        }
    }

    /**
     * Force server to close current client subscription connection to the server
     * @param name Subscription name
     */
    public void dropConnection(String name) {
        dropConnection(name, null);
    }

    /**
     * Force server to close current client subscription connection to the server
     * @param name Subscription name
     * @param database Database to use
     */
    public void dropConnection(String name, String database) {
        RequestExecutor requestExecutor = _store.getRequestExecutor(ObjectUtils.firstNonNull(database, _store.getDatabase()));

        DropSubscriptionConnectionCommand command = new DropSubscriptionConnectionCommand(name);
        requestExecutor.execute(command);
    }
}
