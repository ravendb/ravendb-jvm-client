package net.ravendb.client.documents.operations.queueSink;

import org.apache.commons.lang3.StringUtils;

/**
 * Helpers for encoding Azure Service Bus sources as the strings stored in
 * {@link QueueSinkScript#getQueues()}.
 *
 * <p>Encoding convention:</p>
 * <ul>
 *   <li>{@code "queueName"} — a Service Bus queue.</li>
 *   <li>{@code "topicName;subscriptionName"} — a topic subscription.</li>
 * </ul>
 *
 * <p>
 * The semicolon is used as the separator because Service Bus naming rules forbid
 * {@code ;} in queue, topic, and subscription names, so the delimiter is collision-safe.
 * </p>
 */
public class AzureServiceBusSinkSource {

    private static final char SEPARATOR = ';';

    private AzureServiceBusSinkSource() {
    }

    /**
     * Returns the encoded entry for a queue. Currently a pass-through; provided for symmetry and future-proofing.
     * @param queueName the Service Bus queue name
     * @return the encoded entry
     */
    public static String queue(String queueName) {
        if (StringUtils.isBlank(queueName)) {
            throw new IllegalArgumentException("Queue name must be non-empty.");
        }

        if (queueName.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Queue name must not contain the '" + SEPARATOR + "' character.");
        }

        return queueName;
    }

    /**
     * Returns the encoded entry for a topic subscription, in the form {@code topic;subscription}.
     * @param topicName the Service Bus topic name
     * @param subscriptionName the subscription name on that topic
     * @return the encoded entry
     */
    public static String subscription(String topicName, String subscriptionName) {
        if (StringUtils.isBlank(topicName)) {
            throw new IllegalArgumentException("Topic name must be non-empty.");
        }

        if (StringUtils.isBlank(subscriptionName)) {
            throw new IllegalArgumentException("Subscription name must be non-empty.");
        }

        if (topicName.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Topic name must not contain the '" + SEPARATOR + "' character.");
        }

        if (subscriptionName.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Subscription name must not contain the '" + SEPARATOR + "' character.");
        }

        return topicName + SEPARATOR + subscriptionName;
    }
}
