package net.ravendb.client.http;

import java.time.LocalDateTime;

public class HttpCacheItem {

    public String changeVector;
    public String payload;
    public LocalDateTime lastServerUpdate;

    public HttpCache cache;

    public HttpCacheItem() {
        this.lastServerUpdate = LocalDateTime.now();
    }

}
