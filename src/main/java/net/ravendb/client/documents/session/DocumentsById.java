package net.ravendb.client.documents.session;

import java.util.*;

public class DocumentsById implements Iterable<Map.Entry<String, DocumentInfo>> {

    private final Map<String, DocumentInfo> _inner;

    public DocumentsById() {
        this._inner = new TreeMap<>(String::compareToIgnoreCase);
    }

    public DocumentInfo getValue(String id) {
        return _inner.get(id);
    }

    public void add(DocumentInfo info) {
        if (_inner.containsKey(info.getId())) {
            return;
        }

        _inner.put(info.getId(), info);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean remove(String id) {
        return _inner.remove(id) != null;
    }

    public void clear() {
        _inner.clear();
    }

    public int getCount() {
        return _inner.size();
    }

    @Override
    public Iterator<Map.Entry<String, DocumentInfo>> iterator() {
        return _inner.entrySet().iterator();
    }
}
