package net.ravendb.client.http;

import java.time.Duration;

public class AggressiveCacheOptions {
    private Duration duration;
    private AggressiveCacheMode mode;

    public AggressiveCacheOptions(Duration duration, AggressiveCacheMode mode) {
        this.duration = duration;
        this.mode = mode;
    }

    public AggressiveCacheMode getMode() {
        return mode;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setMode(AggressiveCacheMode mode) {
        this.mode = mode;
    }
}
