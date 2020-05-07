package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiDatabaseHiLoIdGenerator {

    protected final DocumentStore store;

    private final ConcurrentMap<String, MultiTypeHiLoIdGenerator> _generators = new ConcurrentHashMap<>();

    public MultiDatabaseHiLoIdGenerator(DocumentStore store) {
        this.store = store;
    }

    public String generateDocumentId(String database, Object entity) {
        String db = database != null ? database : store.getDatabase();

        MultiTypeHiLoIdGenerator generator = _generators.computeIfAbsent(db, x -> generateMultiTypeHiLoFunc(x));
        return generator.generateDocumentId(entity);
    }

    public MultiTypeHiLoIdGenerator generateMultiTypeHiLoFunc(String database) {
        return new MultiTypeHiLoIdGenerator(store, database);
    }

    public void returnUnusedRange() {
        for (MultiTypeHiLoIdGenerator generator : _generators.values()) {
            generator.returnUnusedRange();
        }
    }

}
