package net.ravendb.client.documents.indexes;

import java.util.HashMap;

public class IndexConfiguration extends HashMap<String, String> {
    public void setSetting(String key, String value) {
        this.put(key, value);
    }
}
