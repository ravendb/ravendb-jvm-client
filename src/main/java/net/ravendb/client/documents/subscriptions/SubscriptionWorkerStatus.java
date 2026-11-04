package net.ravendb.client.documents.subscriptions;

import java.util.Date;

/**
 * An immutable snapshot of what a subscription worker was doing at a single point in time. Taking a snapshot
 * keeps the state and the failure that produced it consistent with each other, which reading two separate
 * properties would not.
 */
public final class SubscriptionWorkerStatus {

    private final SubscriptionWorkerState state;
    private final Exception exception;
    private final Date sinceUtc;
    private final Date failingSinceUtc;

    SubscriptionWorkerStatus(SubscriptionWorkerState state, Exception exception, Date sinceUtc, Date failingSinceUtc) {
        this.state = state;
        this.exception = exception;
        this.sinceUtc = sinceUtc;
        this.failingSinceUtc = failingSinceUtc;
    }

    /**
     * @return The stage the worker was in.
     */
    public SubscriptionWorkerState getState() {
        return state;
    }

    /**
     * The failure that led to the current state. Set for {@link SubscriptionWorkerState#RETRYING} and
     * {@link SubscriptionWorkerState#FAULTED}, null otherwise. On a worker that keeps failing this
     * is the most recent failure, which is not necessarily the one that started the trouble - see
     * {@link #getFailingSinceUtc()}.
     * @return failure that led to the current state
     */
    public Exception getException() {
        return exception;
    }

    /**
     * When the worker entered this state, and nothing more than that. Reaching the same state again does not
     * move it, so a worker that keeps processing batches reports the time its current batch started. A worker
     * that cannot reach the server, however, alternates between {@link SubscriptionWorkerState#CONNECTING}
     * and {@link SubscriptionWorkerState#RETRYING}, so this moves on every attempt. Use
     * {@link #getFailingSinceUtc()} to tell how long it has been in trouble.
     * @return when the worker entered this state
     */
    public Date getSinceUtc() {
        return sinceUtc != null ? new Date(sinceUtc.getTime()) : null;
    }

    /**
     * When the worker last stopped being able to talk to the server, or null while it is connected.
     * Unlike {@link #getSinceUtc()} this survives the whole reconnect cycle, so it answers "how long has this
     * worker been broken?" for a worker that is retrying in a loop. It is cleared as soon as the worker is
     * connected again, which makes a non-null value the thing to alert on.
     * @return when the worker started failing, or null
     */
    public Date getFailingSinceUtc() {
        return failingSinceUtc != null ? new Date(failingSinceUtc.getTime()) : null;
    }

    Date failingSinceUtcInternal() {
        return failingSinceUtc;
    }

    @Override
    public String toString() {
        String text = state + " since " + sinceUtc.toInstant();

        if (failingSinceUtc != null) {
            text += ", failing since " + failingSinceUtc.toInstant();
        }

        if (exception != null) {
            text += " because of " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }

        return text;
    }
}
