package net.ravendb.client.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpCache implements CleanCloseable {

    private Cache<String, HttpCacheItem> items;

    public HttpCache(int size) {
        items = CacheBuilder.newBuilder()
                .softValues()
                .maximumWeight(size)
                .weigher((String k, HttpCacheItem v) -> v.payload != null ?  v.payload.length() + 20 : 20)
                .build();
    }

    @Override
    public void close() {
        items.invalidateAll();
        items = null;
    }

    public void clear() {
        items.invalidateAll();
    }

    public final AtomicInteger generation = new AtomicInteger();

    public long getNumberOfItems() {
        return items.size();
    }

    public void set(String url, String changeVector, String result) {
        HttpCacheItem httpCacheItem = new HttpCacheItem();
        httpCacheItem.changeVector = changeVector;
        httpCacheItem.payload = result;
        httpCacheItem.cache = this;
        httpCacheItem.generation = generation.get();

        items.put(url, httpCacheItem);
    }

    public ReleaseCacheItem get(String url, Reference<String> changeVectorRef, Reference<String> responseRef) {
        HttpCacheItem item = items.getIfPresent(url);
        if (item != null) {
            changeVectorRef.value = item.changeVector;
            responseRef.value = item.payload;

            return new ReleaseCacheItem(item);
        }

        changeVectorRef.value = null;
        responseRef.value = null;
        return new ReleaseCacheItem();
    }

    public void setNotFound(String url) {
        HttpCacheItem httpCacheItem = new HttpCacheItem();
        httpCacheItem.changeVector = "404 response";
        httpCacheItem.cache = this;
        httpCacheItem.generation = generation.get();

        httpCacheItem.flags = EnumSet.of(ItemFlags.NOT_FOUND);

        items.put(url, httpCacheItem);
    }

    public static class ReleaseCacheItem implements CleanCloseable {
        public final HttpCacheItem item;
        private final int _cacheGeneration;

        public ReleaseCacheItem() {
            item = null;
            _cacheGeneration = 0;
        }

        public ReleaseCacheItem(HttpCacheItem item) {
            this.item = item;
            this._cacheGeneration = item.cache.generation.get();
        }

        public void notModified() {
            if (item != null) {
                item.lastServerUpdate = LocalDateTime.now();
                item.generation = _cacheGeneration;
            }
        }

        public Duration getAge() {
            if (item == null) {
                return Duration.ofMillis(Long.MAX_VALUE);
            }
            return Duration.between(item.lastServerUpdate, LocalDateTime.now());
        }

        public boolean getMightHaveBeenModified() {
            return item.generation != _cacheGeneration;
        }

        @SuppressWarnings("EmptyMethod")
        @Override
        public void close() {
        }
    }
}
