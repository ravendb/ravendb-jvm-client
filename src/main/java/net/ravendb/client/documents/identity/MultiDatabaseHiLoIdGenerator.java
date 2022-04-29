package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiDatabaseHiLoIdGenerator implements IHiLoIdGenerator {

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

    @Override
    public long generateNextIdFor(String database, Object entity) {
        String collectionName = store.getConventions().getCollectionName(entity);
        return generateNextIdFor(database, collectionName);
    }

    @Override
    public long generateNextIdFor(String database, Class<?> type) {
        String collectionName = store.getConventions().getCollectionName(type);
        return generateNextIdFor(database, collectionName);
    }

    @Override
    public long generateNextIdFor(String database, String collectionName) {
        database = store.getEffectiveDatabase(database);
        MultiTypeHiLoIdGenerator generator = _generators.computeIfAbsent(database, this::generateMultiTypeHiLoFunc);
        return generator.generateNextIdFor(collectionName);
    }
}
