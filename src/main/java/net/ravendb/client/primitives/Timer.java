package net.ravendb.client.primitives;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Timer implements CleanCloseable {
    private final Runnable action;
    private ScheduledFuture<Void> scheduledFuture;
    private Duration period;

    public Timer(Runnable action, Duration dueTime) {
        this(action, dueTime, null);
    }

    public Timer(Runnable action, Duration dueTime, Duration period) {
        this.action = action;
        this.period = period;
        schedule(dueTime);
    }

    public void change(Duration dueTime) {
        change(dueTime, null);
    }

    public void change(Duration dueTime, Duration period) {
        this.period = period;
        this.scheduledFuture.cancel(false);
        this.schedule(dueTime);
    }

    private void schedule(Duration dueTime) {
        this.scheduledFuture = TimerService.service.schedule(() -> {
            CompletableFuture.runAsync(() -> {
                if (period != null) {
                    schedule(period);
                }
                this.action.run();
            });
            return null;
        }, dueTime.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
        }
    }
}
