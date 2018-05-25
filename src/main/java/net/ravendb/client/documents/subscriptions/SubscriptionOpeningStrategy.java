package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Options for opening a subscription
 */
@UseSharpEnum
public enum SubscriptionOpeningStrategy {

    /**
     * The client will successfully open a subscription only if there isn't any other currently connected client.
     * Otherwise it will end up with SubscriptionInUseException.
     */
    OPEN_IF_FREE,

    /**
     * The connecting client will successfully open a subscription even if there is another active subscription's consumer.
     * If the new client takes over an existing client then the existing one will get a SubscriptionInUseException.
     *
     * The subscription will always be held by the last connected client.
     */
    TAKE_OVER,

    /**
     * If the client currently cannot open the subscription because it is used by another client but it will wait for that client
     * to complete and keep attempting to gain the subscription
     */
    WAIT_FOR_FREE
}
