package net.ravendb.client.documents.subscriptions;

import org.apache.commons.lang3.StringUtils;

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

    private SubscriptionWorkerOptions() {
        strategy = SubscriptionOpeningStrategy.OPEN_IF_FREE;
        maxDocsPerBatch = 4096;
        timeToWaitBeforeConnectionRetry = Duration.ofSeconds(5);
        maxErroneousPeriod = Duration.ofMinutes(5);
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
     * @return Max amount that the server will try to retriev and send to client. Default: 4096
     */
    public int getMaxDocsPerBatch() {
        return maxDocsPerBatch;
    }

    /**
     * @param maxDocsPerBatch Max amount that the server will try to retriev and send to client. Default: 4096
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
     * That's a useful practice for ad-hoc, one-time, persistant data processing.
     */
    public boolean isCloseWhenNoDocsLeft() {
        return closeWhenNoDocsLeft;
    }

    /**
     * Will continue the subscription work until the server have no more new documents to send.
     * That's a useful practice for ad-hoc, one-time, persistant data processing.
     */
    public void setCloseWhenNoDocsLeft(boolean closeWhenNoDocsLeft) {
        this.closeWhenNoDocsLeft = closeWhenNoDocsLeft;
    }
}
