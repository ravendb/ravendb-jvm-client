package net.ravendb.abstractions.data;

/**
 * Options for opening a subscription
 */
public enum SubscriptionOpeningStrategy {

    /**
     * The client will successfully open a subscription only if there isn't any other currently connected client.
     * Otherwise it will end up with SubscriptionInUseException.
     */
    OPEN_IF_FREE,

    /**
     * The connecting client will successfully open a subscription even if there is another active subscription's consumer.
     * If the new client takes over the subscription then the existing one will get rejected.
     * The subscription will always be processed by the last connected client.
     */
    TAKE_OVER,

    /**
     * The client opening a subscription with Forced strategy set will always get it and keep it open until another client with the same strategy gets connected.
     */
    FORCE_AND_KEEP,

    /**
     * If the client currently cannot open the subscription because it is used by another client then it will subscribe Changes API to be notified about subscription status changes.
     * Every time SubscriptionReleased notification arrives, it will repeat an attempt to open the subscription. After it succeeds in opening, it will process docs as usual.
     */
    WAIT_FOR_FREE
}
