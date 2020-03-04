package net.ravendb.client.http;

import java.time.LocalDateTime;
import java.util.EnumSet;

public class HttpCacheItem {

    public String changeVector;
    public String payload;
    public LocalDateTime lastServerUpdate;
    public int generation;
    public EnumSet<ItemFlags> flags = EnumSet.of(ItemFlags.NONE);

    public HttpCache cache;

    public HttpCacheItem() {
        this.lastServerUpdate = LocalDateTime.now();
    }

}
