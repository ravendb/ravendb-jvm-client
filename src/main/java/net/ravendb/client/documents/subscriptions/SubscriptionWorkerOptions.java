package net.ravendb.client.documents.subscriptions;

import org.apache.commons.lang3.StringUtils;

import java.net.Socket;
import java.time.Duration;

/**
 * Holds subscription connection properties, control both how client and server side behaves
 */
public class SubscriptionWorkerOptions {

    private String subscriptionName;
    private Duration timeToWaitBeforeConnectionRetry;
    private boolean ignoreSubscriberErrors;
    private SubscriptionOpeningStrategy strategy;
    private int maxDocsPerBatch;
    private Duration maxErroneousPeriod;
    private boolean closeWhenNoDocsLeft;
    private int receiveBufferSize;
    private int sendBufferSize;

    private SubscriptionWorkerOptions() {
        strategy = SubscriptionOpeningStrategy.OPEN_IF_FREE;
        maxDocsPerBatch = 4096;
        timeToWaitBeforeConnectionRetry = Duration.ofSeconds(5);
        maxErroneousPeriod = Duration.ofMinutes(5);
        receiveBufferSize = 32 * 1024;
        sendBufferSize = 32 * 1024;
    }

    /**
     * Create a subscription connection
     * @param subscriptionName Subscription name as received from CreateSubscription
     */
    public SubscriptionWorkerOptions(String subscriptionName) {
        this();

        if (StringUtils.isEmpty(subscriptionName)) {
            throw new IllegalArgumentException("SubscriptionName cannot be null or empty");
        }

        this.subscriptionName = subscriptionName;
    }

    /**
     * Subscription name as received from CreateSubscription
     * @return Subscription name
     */
    public String getSubscriptionName() {
        return subscriptionName;
    }

    /**
     * @return Cooldown time between connection retry. Default: 5 seconds
     */
    public Duration getTimeToWaitBeforeConnectionRetry() {
        return timeToWaitBeforeConnectionRetry;
    }

    /**
     * @param timeToWaitBeforeConnectionRetry Cooldown time between connection retry. Default: 5 seconds
     */
    public void setTimeToWaitBeforeConnectionRetry(Duration timeToWaitBeforeConnectionRetry) {
        this.timeToWaitBeforeConnectionRetry = timeToWaitBeforeConnectionRetry;
    }

    /**
     * @return Whether subscriber error should halt the subscription processing or not. Default: false
     */
    public boolean isIgnoreSubscriberErrors() {
        return ignoreSubscriberErrors;
    }

    /**
     * @param ignoreSubscriberErrors Whether subscriber error should halt the subscription processing or not. Default: false
     */
    public void setIgnoreSubscriberErrors(boolean ignoreSubscriberErrors) {
        this.ignoreSubscriberErrors = ignoreSubscriberErrors;
    }

    /**
     * @return How connection attempt handle existing\incoming connection. Default: OPEN_IF_FREE
     */
    public SubscriptionOpeningStrategy getStrategy() {
        return strategy;
    }

    /**
     * @param strategy How connection attempt handle existing\incoming connection. Default: OPEN_IF_FREE
     */
    public void setStrategy(SubscriptionOpeningStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @return Max amount that the server will try to retrieve and send to client. Default: 4096
     */
    public int getMaxDocsPerBatch() {
        return maxDocsPerBatch;
    }

    /**
     * @param maxDocsPerBatch Max amount that the server will try to retrieve and send to client. Default: 4096
     */
    public void setMaxDocsPerBatch(int maxDocsPerBatch) {
        this.maxDocsPerBatch = maxDocsPerBatch;
    }

    /**
     * @return Maximum amount of time during which a subscription connection may be in erroneous state. Default: 5 minutes
     */
    public Duration getMaxErroneousPeriod() {
        return maxErroneousPeriod;
    }

    /**
     * @param maxErroneousPeriod Maximum amount of time during which a subscription connection may be in erroneous state. Default: 5 minutes
     */
    public void setMaxErroneousPeriod(Duration maxErroneousPeriod) {
        this.maxErroneousPeriod = maxErroneousPeriod;
    }

    /**
     * Will continue the subscription work until the server have no more new documents to send.
     * That's a useful practice for ad-hoc, one-time, persistent data processing.
     * @return true, if subscription should be closed after processing all documents
     */
    public boolean isCloseWhenNoDocsLeft() {
        return closeWhenNoDocsLeft;
    }

    /**
     * Will continue the subscription work until the server have no more new documents to send.
     * That's a useful practice for ad-hoc, one-time, persistent data processing.
     * @param closeWhenNoDocsLeft true, if subscription should be closed after processing all documents
     */
    public void setCloseWhenNoDocsLeft(boolean closeWhenNoDocsLeft) {
        this.closeWhenNoDocsLeft = closeWhenNoDocsLeft;
    }

    /**
     * @return Receive buffer size for the underlying {@link Socket}. Default: 32 kB
     */
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Set receive buffer size for the underlying {@link Socket}. See {@link Socket#setReceiveBufferSize(int)}.
     * Optimal size is a trade-off between memory and (size of the link in B/s) x (round trip delay in seconds)
     * @param receiveBufferSize receive buffer size for the underlying {@link Socket}. Default: 32 kB
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * @return Send buffer size for the underlying {@link Socket}. Default: 32 kB
     */
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * Set send buffer size for the underlying {@link Socket}. See {@link Socket#setSendBufferSize(int)}.
     * Optimal size is a trade-off between memory and (size of the link in B/s) x (round trip delay in seconds)
     * @param sendBufferSize receive buffer size for the underlying {@link Socket}. Default: 32 kB
     */
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }
}
