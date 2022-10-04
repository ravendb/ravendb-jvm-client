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

    public Map<String, EntityInfo> getTrackedEntities(InMemoryDocumentSessionOperations session) {
        Map<String, EntityInfo> result = new TreeMap<>(String::compareToIgnoreCase);

        for (Map.Entry<String, DocumentInfo> keyValue : _inner.entrySet()) {
            EntityInfo entityInfo = new EntityInfo();
            entityInfo.setId(keyValue.getKey());
            entityInfo.setEntity(keyValue.getValue().getEntity());
            entityInfo.setDeleted(session.isDeleted(keyValue.getKey()));
            result.put(keyValue.getKey(), entityInfo);
        }

        return result;
    }

    public static class EntityInfo {
        private String id;
        private Object entity;
        private boolean isDeleted;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object getEntity() {
            return entity;
        }

        public void setEntity(Object entity) {
            this.entity = entity;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public void setDeleted(boolean deleted) {
            isDeleted = deleted;
        }
    }
}
