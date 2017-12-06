package net.ravendb.client.http;

import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;

//TODO:
public class HttpCache implements CleanCloseable {

    public HttpCache(int size) {
        //TODO:
    }

    @Override
    public void close() {

    }

    public void set(String url, String changeVector, String result) {
        //TODO:
    }

    public ReleaseCacheItem get(String url, Reference<String> cachedChangeVector, Reference<String> cachedValue) {
        return new ReleaseCacheItem();
    }

    public void setNotFound(String url) {
    }

    public static class ReleaseCacheItem implements CleanCloseable {
        public void notModified() {
            //TODO:
        }

        public Duration getAge() {
            return null;
        }

        public boolean getMightHaveBeenModified() {
            return false;
        }

        @Override
        public void close() {
            //TODO:
        }
    }
}
