package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * The stage a subscription worker is currently in. Read it through {@link AbstractSubscriptionWorker#getStatus()}.
 */
@UseSharpEnum
public enum SubscriptionWorkerState {
    /**
     * The worker was created but run was not called yet, so it is not doing anything.
     */
    NOT_STARTED,

    /**
     * Opening a connection to the server, which includes resolving the node that owns the subscription and
     * the TCP handshake. This is also the state while reconnecting after a failure.
     */
    CONNECTING,

    /**
     * Connected to the server and waiting for it to send the next batch. The worker is healthy and idle -
     * there is nothing for it to process right now.
     */
    WAITING_FOR_DOCUMENTS,

    /**
     * A batch was received and handed to the subscriber callback. The state stays here until the callback
     * returned and the batch was acknowledged to the server.
     */
    PROCESSING,

    /**
     * The connection failed and the worker is going to reconnect.
     * {@link SubscriptionWorkerStatus#getException()} holds the failure that caused it.
     */
    RETRYING,

    /**
     * The worker gave up - the failure it hit cannot be recovered from by reconnecting, so it stopped and the
     * future returned from run completed with that failure. {@link SubscriptionWorkerStatus#getException()}
     * holds it. This state is terminal.
     */
    FAULTED,

    /**
     * The worker stopped on request - it was closed. This state is terminal.
     */
    STOPPED
}
