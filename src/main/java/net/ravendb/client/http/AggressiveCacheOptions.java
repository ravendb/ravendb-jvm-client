package net.ravendb.client.http;

import java.time.Duration;

public class AggressiveCacheOptions {
    private final Duration duration;

    public Duration getDuration() {
        return duration;
    }

    public AggressiveCacheOptions(Duration duration) {
        this.duration = duration;
    }
}
