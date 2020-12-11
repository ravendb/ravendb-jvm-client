package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiDatabaseHiLoIdGenerator {

    protected final DocumentStore store;

    private final ConcurrentMap<String, MultiTypeHiLoIdGenerator> _generators = new ConcurrentHashMap<>();

    public MultiDatabaseHiLoIdGenerator(DocumentStore store) {
        this.store = store;
    }

    public String generateDocumentId(String database, Object entity) {
        database = store.getEffectiveDatabase(database);
        MultiTypeHiLoIdGenerator generator = _generators.computeIfAbsent(database, x -> generateMultiTypeHiLoFunc(x));
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
