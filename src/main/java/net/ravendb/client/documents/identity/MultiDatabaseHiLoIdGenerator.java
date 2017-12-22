package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiDatabaseHiLoIdGenerator {

    protected final DocumentStore store;
    protected final DocumentConventions conventions;

    private final ConcurrentMap<String, MultiTypeHiLoIdGenerator> _generators = new ConcurrentHashMap<>();

    public MultiDatabaseHiLoIdGenerator(DocumentStore store, DocumentConventions conventions) {
        this.store = store;
        this.conventions = conventions;
    }

    public String generateDocumentId(String dbName, Object entity) {
        String db = dbName != null ? dbName : store.getDatabase();

        MultiTypeHiLoIdGenerator generator = _generators.computeIfAbsent(db, x -> generateMultiTypeHiLoFunc(x));
        return generator.generateDocumentId(entity);
    }

    public MultiTypeHiLoIdGenerator generateMultiTypeHiLoFunc(String dbName) {
        return new MultiTypeHiLoIdGenerator(store, dbName, conventions);
    }

    public void returnUnusedRange() {
        for (MultiTypeHiLoIdGenerator generator : _generators.values()) {
            generator.returnUnusedRange();
        }
    }

}
