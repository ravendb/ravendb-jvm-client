package net.ravendb.client.documents.indexes;

import java.util.HashMap;

public final class IndexSchemaDefinitions extends HashMap<String, String> {

    @Override
    public String put(String key, String value) {
        return super.put(key, value);
    }

    @Override
    public String get(Object key) {
        return super.get(key);
    }

    public void add(String key, String value) {
        super.put(key, value);
    }

    public String get(String key, String defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        add(key, value);
    }
}

